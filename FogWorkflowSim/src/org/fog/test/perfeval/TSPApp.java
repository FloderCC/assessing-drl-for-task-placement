package org.fog.test.perfeval;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.TSPController;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.workflowsim.CondorVM;
import org.workflowsim.TSPWorkflowPlanner;
import org.workflowsim.WorkflowEngine;
import org.workflowsim.utils.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * This test class simulates a task scheduling and placement application with a cloud device and a heterogeneous fog
 * layer made up of several devices
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
public class TSPApp {

    /** Environment setup **/

    // Cloud setup
    static int numCloudDevices = 0;
    //{MIPS, RAM (MB), Storage (MB), Busy power, Idle power}
    static double[] cloudNodeFeatures = new double[]{73440 * 2, 16 * 1024 * 2, 120 * 1024, 222, 52};
    static int cloudNodesUploadBandwidth  = 200;
    static int cloudNodesDownloadBandwidth  = 200;

    // Fog setup
    static int numFogDevices = 30;
    static int episodeNumber = 0;

    // {MIPS, RAM (MB), Storage (MB), Busy power, Idle power}
    static double[][] availableFogNodesFeatures = new double[][]{
            {46880 * 2, 4 * 1024, 128 * 1024, 245, 88.4},
            {46880 * 2, 2 * 1024, 128 * 1024, 245, 88.4},
            {7320, 4 * 1024, 120 * 1024, 117, 86},
            {7320, 2 * 1024, 120 * 1024, 117, 86},
    };

    static double[][] fogNodesFeatures;
    static int fogNodesUploadBandwidth  = 1000;
    static int fogNodesDownloadBandwidth  = 1000;

    // Mobile setup
    static int numMobileDevices = 1; //fixed for simulation the gateway metrics
    static double[] gatewayNodeFeatures = new double[]{5320, 2 * 1024, 64 * 1024, 135, 93.7};//{MIPS, RAM (MB), Storage (MB), Busy power, Idle power}
    static double myRealGatewayMips = 8710; //obtained with 7-Zip for i7-6500U CPU @ 2.50GHz

    static double latencyMobileGateway = 20; //not used in this case

    static double latencyGatewayFogNode = 0.050;
    static double latencyGatewayCloudNode = 0.100;

    // Scheduling setup
    final static String[] algorithmStr = new String[]{
            "TSP_Placement",
            "TSP_Scheduling_Placement",
    };

    // Simulation parameters
    static String schedulerMethod = null;
    // Agent selection. This needs to match wit the selected strategy
    static Parameters.TSPStrategy stp_strategy = null;
    static boolean consider_gateway_computation_time = true;
    static String taskPath = null;

    /** Simulator variables **/

    // Default variables
    final static int numDepths = 1; //num depths in the fog layer
    final static int numMobilesPerDept = 1;
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Double[]> record= new ArrayList<>();
    private static WorkflowEngine wfEngine;
    private static TSPController controller;

    public static void simulate(double deadline) {

        FogUtils.set1();

        fogNodesFeatures = new double[numFogDevices][];

        Random generator = new Random(42);

        for (int i=0; i < TSPApp.numFogDevices; i++){
            int random =generator.nextInt(availableFogNodesFeatures.length);
            fogNodesFeatures[i]=new double[]{availableFogNodesFeatures[random][0], availableFogNodesFeatures[random][1], availableFogNodesFeatures[random][2], availableFogNodesFeatures[random][3], availableFogNodesFeatures[random][3]};
        }

        try {
            //Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "TSP"; // identifier of the application

            createFogDevices(1, appId);

            List<? extends Host> hostlist = new ArrayList<Host>();
            int hostnum = 0;
            for(FogDevice device : fogDevices){
                hostnum += device.getHostList().size();
                hostlist.addAll(device.getHostList());
            }
            int vmNum = hostnum;//number of vms;

            File taskFile = new File(taskPath);
            if (!taskFile.exists()) {
                System.out.println("Error: Please replace taskPath with the physical path in your working environment!");
                return;
            }

            Parameters.SchedulingAlgorithm sch_method =Parameters.SchedulingAlgorithm.valueOf(schedulerMethod);
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.INVALID;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.SHARED;

            /**
             * Setting the placement strategy
             */

            Parameters.setTSPStrategy(stp_strategy);

            /**
             * No overheads
             */
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);

            /**
             * Initialize static parameters
             */
            Parameters.init(vmNum, taskPath, null, null, op, null, sch_method, null, pln_method, null, 0,
                    false, 5, false, consider_gateway_computation_time, episodeNumber);
            ReplicaCatalog.init(file_system);

            /**
             * Specifying that it is a TSP problem
             */
            Parameters.setIsTsp(true);

            /**
             * Create a WorkflowPlanner with one scheduler.
             */
            TSPWorkflowPlanner wfPlanner = new TSPWorkflowPlanner("planner_0", 1);
            /**
             * Create a WorkflowEngine.
             */
            wfEngine = wfPlanner.getWorkflowEngine();

            /**
             * Set a offloading Strategy for OffloadingEngine
             */

            wfEngine.getoffloadingEngine().setOffloadingStrategy(null);

            /**
             * Set a deadline of workflow for WorkflowEngine
             */
            wfEngine.setDeadLine(deadline);
            /**
             * Create a list of VMs.The userId of a vm is basically the id of
             * the scheduler that controls this vm.
             */
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), Parameters.getVmNum(), hostlist);

            hostlist = null;//清空，释放内存
            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmlist0, 0);
            vmlist0 = null;

            controller = new TSPController("master-controller", fogDevices, wfEngine);

            /**
             * Binds the data centers with the scheduler.
             */
            List<PowerHost> list;
            for(FogDevice fogdevice:controller.getFogDevices()){
                wfEngine.bindSchedulerDatacenter(fogdevice.getId(), 0);
                list = fogdevice.getHostList();  //输出设备上的主机
                System.out.println(fogdevice.getName()+": ");
                for (PowerHost host : list){
                    System.out.print(host.getId()+":Mips("+host.getTotalMips()+"),"+"cost("+host.getcostPerMips()+")  ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

    private static void createFogDevices(int userId, String appId) {

        double ratePerMips = 0.96;
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1; // the cost of using storage in this resource
        double costPerBw = 0.2;//

        List<Long> GHzList = new ArrayList<>();
        List<Double> CostList = new ArrayList<>();

        int fog_parent = -1;

        if (numCloudDevices >0){
            for (int i = 0; i < numCloudDevices; i++){ //setup cloud capacities
                GHzList.add((long)cloudNodeFeatures[0]); //MIPS
                CostList.add(0.96);
            }

            FogDevice cloud = createFogDevice("cloud", GHzList.size(), GHzList, CostList,
                    (int) cloudNodeFeatures[1], cloudNodesUploadBandwidth, cloudNodesDownloadBandwidth, 0, ratePerMips, cloudNodeFeatures[3], cloudNodeFeatures[4], costPerMem,costPerStorage,costPerBw, (long) cloudNodeFeatures[2]);
            cloud.setParentId(-1);
            cloud.setUplinkLatency(latencyGatewayCloudNode);
            fogDevices.add(cloud);

            fog_parent = cloud.getId();
        }

        for(int i = 0; i< numDepths; i++){
            addFogNodes(i+"", userId, appId, fog_parent); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
        }
    }

    private static FogDevice addFogNodes(String id, int userId, String appId, int parentId){
        double ratePerMips = 0.48;
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1; // the cost of using storage in this resource
        double costPerBw = 0.1;

        //{MIPS, RAM (MB), Storage (MB), Busy power, Idle power}

        List<Long> GHzList = new ArrayList<>();
        List<Long> RamList = new ArrayList<>();
        List<Long> StorageList = new ArrayList<>();
        List<Long> BPowerList = new ArrayList<>();
        List<Long> IPowerList = new ArrayList<>();
        List<Double> CostList = new ArrayList<>();

        for (int i = 0; i < numFogDevices; i++){ //setup fog capacities
            GHzList.add((long)fogNodesFeatures[i][0]);
            RamList.add((long)fogNodesFeatures[i][1]);
            StorageList.add((long)fogNodesFeatures[i][2]);
            BPowerList.add((long)fogNodesFeatures[i][3]);
            IPowerList.add((long)fogNodesFeatures[i][4]);
            CostList.add(0.48);
        }

        FogDevice dept = createFogDevice("f-"+id, numFogDevices, GHzList, CostList,
                RamList, fogNodesUploadBandwidth, fogNodesDownloadBandwidth, 1, ratePerMips, BPowerList, IPowerList, costPerMem, costPerStorage, costPerBw, StorageList);

        fogDevices.add(dept);
        dept.setParentId(parentId);
        dept.setUplinkLatency(latencyGatewayFogNode); // latency of connection between gateways and server is 4 ms
        for(int i=0;i<numMobilesPerDept;i++){
            String mobileId = id+"-"+i;
            FogDevice mobile = addGateway(mobileId, userId, appId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(latencyMobileGateway);
            fogDevices.add(mobile);
        }
        return dept;
    }

    private static FogDevice addGateway(String id, int userId, String appId, int parentId){
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1; // the cost of using storage in this resource
        double costPerBw = 0.3;//每带宽的花费

        List<Long> GHzList = new ArrayList<>();
        List<Double> CostList = new ArrayList<>();

        for (int i = 0; i < numMobileDevices; i++){ //setup cloud capacities
            CostList.add(0.0);
            GHzList.add((long)gatewayNodeFeatures[0]);
        }
        FogDevice mobile = createFogDevice("m-"+id, GHzList.size(), GHzList, CostList,
                (int)gatewayNodeFeatures[1], 20*1024, 40*1024, 3, 0, gatewayNodeFeatures[3], gatewayNodeFeatures[4],costPerMem,costPerStorage,costPerBw,(int)gatewayNodeFeatures[2]);
        mobile.setParentId(parentId);
        return mobile;
    }

    /**
     * Creates a vanilla fog device
     * @param nodeName name of the device to be used in simulation
     * @param hostnum the number of the host of device
     * @param mips the list of host'MIPS
     * @param costPerMips the list of host'cost per mips
     * @param ram RAM
     * @param upBw uplink bandwidth (Kbps)
     * @param downBw downlink bandwidth (Kbps)
     * @param level hierarchy level of the device
     * @param ratePerMips cost rate per MIPS used
     * @param busyPower(mW)
     * @param idlePower(mW)
     * @return
     */
    private static FogDevice createFogDevice(String nodeName, int hostnum, List<Long> mips, List<Double> costPerMips,
                                             int ram, long upBw, long downBw, int level, double ratePerMips,
                                             double busyPower, double idlePower,
                                             double costPerMem,double costPerStorage,double costPerBw,long storage) {

        List<Host> hostList = new ArrayList<Host>();

        for ( int i = 0 ;i < hostnum; i++ )
        {
            List<Pe> peList = new ArrayList<Pe>();
            // Create PEs and add these into a list.
            peList.add(new Pe(0, new PeProvisionerSimple(mips.get(i)))); // need to store Pe id and MIPS Rating
            int hostId = FogUtils.generateEntityId();
            int bw = 10000;

            PowerHost host = new PowerHost(
                    hostId,
                    costPerMips.get(i),
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList),
                    new FogLinearPowerModel(busyPower, idlePower)//默认发送功率100mW 接收功率25mW
            );

            hostList.add(host);
        }

        // Create a DatacenterCharacteristics object
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource每秒的花费
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;

        // Finally, we need to create a storage object.
        try {
            HarddriveStorage s1 = new HarddriveStorage(nodeName, 1e12);
            storageList.add(s1);

            fogdevice = new FogDevice(nodeName, characteristics,
                    new VmAllocationPolicySimple(hostList), storageList, 10, upBw, downBw, 0, ratePerMips); //TSP Comment. This is the layer placement class
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }

    /**
     * Creates a vanilla fog device
     * @param nodeName name of the device to be used in simulation
     * @param hostnum the number of the host of device
     * @param mips the list of host'MIPS
     * @param costPerMips the list of host'cost per mips
     * @param rams the list of RAM
     * @param upBw uplink bandwidth (Kbps)
     * @param downBw downlink bandwidth (Kbps)
     * @param level hierarchy level of the device
     * @param ratePerMips cost rate per MIPS used
     * @param busyPowers(mW) the list of busy powers
     * @param idlePowers(mW) the list of idle powers
     * @return
     */
    private static FogDevice createFogDevice(String nodeName, int hostnum, List<Long> mips, List<Double> costPerMips,
                                             List<Long> rams, long upBw, long downBw, int level, double ratePerMips,
                                             List<Long> busyPowers, List<Long> idlePowers,
                                             double costPerMem,double costPerStorage,double costPerBw,List<Long>  storages) {

        List<Host> hostList = new ArrayList<Host>();

        for ( int i = 0 ;i < hostnum; i++ )
        {
            List<Pe> peList = new ArrayList<Pe>();
            // Create PEs and add these into a list.
            peList.add(new Pe(0, new PeProvisionerSimple(mips.get(i)))); // need to store Pe id and MIPS Rating
            int hostId = FogUtils.generateEntityId();
            int bw = 10000;

            PowerHost host = new PowerHost(
                    hostId,
                    costPerMips.get(i),
                    new RamProvisionerSimple(Math.toIntExact(rams.get(i))),
                    new BwProvisionerSimple(bw),
                    storages.get(i),
                    peList,
                    new VmSchedulerTimeShared(peList),
                    new FogLinearPowerModel(busyPowers.get(i), idlePowers.get(i))
            );

            hostList.add(host);
        }

        // Create a DatacenterCharacteristics object
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource每秒的花费
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;

        // Finally, we need to create a storage object.
        try {
            HarddriveStorage s1 = new HarddriveStorage(nodeName, 1e12);
            storageList.add(s1);

            fogdevice = new FogDevice(nodeName, characteristics,
                    new VmAllocationPolicySimple(hostList), storageList, 10, upBw, downBw, 0, ratePerMips); //TSP Comment. This is the layer placement class
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }

    protected static List<CondorVM> createVM(int userId, int vms, List<? extends Host> devicelist) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<CondorVM> list = new LinkedList<>();

        //VM Parameters
        long bw = 1;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        CondorVM[] vm = new CondorVM[vms];
        for (int i = 0; i < vms; i++) {
            double ratio = 1.0;
            int mips = devicelist.get(i).getTotalMips();
            int ram = devicelist.get(i).getRam();
            long size = devicelist.get(i).getStorage();
            vm[i] = new CondorVM(i, userId, mips * ratio, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }

    public static void setSimulationSetup(String scheduler,
                                          Parameters.TSPStrategy strategy,
                                          String dataset,
                                          int fogNodesQuantity,
                                          int episode_number
    ){
        schedulerMethod = scheduler;
        stp_strategy = strategy;
        taskPath = "datasets/" + dataset;
        numFogDevices = fogNodesQuantity;
        episodeNumber = episode_number;

        //reset the variables
        fogDevices = new ArrayList<>();
        record= new ArrayList<>();
    }

    public static void runExplorationMode() throws IOException {
        System.out.println("Starting TSP in Exploration mode...");
        double deadline = Double.MAX_VALUE;

        System.out.println("Staring socket connection...");
        TSPSocketClient.openConnection("192.168.94.145", 5000);

        consider_gateway_computation_time = true;
        String[] datasets = new String[]{
                "1k",
        };
        int [] fogNodesQuantities = new int[]{
                16
        };
        // Setup begin
        Object[][] schedulerStrategyList = new Object[][] {
                {"TSP_Placement", Parameters.TSPStrategy.TP_FIFO},
                {"TSP_Placement", Parameters.TSPStrategy.TP_RANDOM},
                {"TSP_Placement", Parameters.TSPStrategy.TP_ROUND_ROBIN},
                {"TSP_Placement", Parameters.TSPStrategy.TP_DRL},
                {"TSP_Scheduling_Placement", Parameters.TSPStrategy.TSP_DRL}
        };
        int [] randomSeeds = new int[]{
                3,
                7,
                42,
        };

        int numEpisodes = 300;

        boolean drlLoadPretrainedModelOn = false;
        boolean drlTrainingOn = true;
        boolean drlSaveFinalModelOn = false;

        FileWriter csvResultsWriterD = new FileWriter("results_tsp/Results.csv");
        csvResultsWriterD.append("Episode,Dataset,Qty fog nodes,Strategy,Random seed,P1,P2,P3,P4,P5,Simulation time,Avg task time,Total energy,Gateway idle energy,Gateway busy energy,Gateway total energy,Avg Gateway Busy Time,Avg Reward\n");

        int executionNo = 0;
        int setupQuantity = numEpisodes * datasets.length * fogNodesQuantities.length * schedulerStrategyList.length * randomSeeds.length;


        for (String dataset: datasets) {
            for (int fogNodesQuantity : fogNodesQuantities) {
                for (Object[] schedulerStrategy : schedulerStrategyList) {
                    String scheduler = (String) schedulerStrategy[0];
                    Parameters.TSPStrategy strategy = (Parameters.TSPStrategy) schedulerStrategy[1];

                    for (int randomSeed : randomSeeds) {

                        for (int episode_number=0; episode_number < numEpisodes; episode_number++){

                            // defining the simulation environment
                            setSimulationSetup(scheduler, strategy, dataset, fogNodesQuantity, episode_number);
                            simulate(deadline);


                            // initializing the auxiliary variables for job's execution control
                            TSPJobManager.initSimulationVariables(myRealGatewayMips, gatewayNodeFeatures[0], fogDevices, numEpisodes);

                            // logging setup
                            System.out.println("\nInitializing setup " + ++executionNo + "/" + setupQuantity);
                            System.out.println("Dataset: " + dataset);
                            System.out.println("Qty fog nodes: " + fogNodesQuantity);
                            System.out.println("Strategy: " + strategy.name());
                            System.out.println("Random seed: " + randomSeed);

                            System.out.println("DRL training mode: " + "load_pretrained_on = " + drlLoadPretrainedModelOn + " training_on = " + drlTrainingOn + " save_final_model_on = " + drlSaveFinalModelOn);

                            // sending the server configuration
                            String setupName = dataset.substring(dataset.lastIndexOf("(") + 1, dataset.length() - 1) + "-" + strategy.name() + "-" + fogNodesQuantity + "-" + randomSeed;

                            System.out.println("Sending server configuration..");
                            if (episode_number == 0){
                                TSPSocketClient.sendSeversSetup(setupName, strategy.name(), (numCloudDevices == 0)?null:cloudNodeFeatures, fogNodesFeatures, 5, randomSeed, drlLoadPretrainedModelOn, drlTrainingOn, drlSaveFinalModelOn, cloudNodesUploadBandwidth, cloudNodesDownloadBandwidth, fogNodesUploadBandwidth, fogNodesDownloadBandwidth, latencyGatewayFogNode, latencyGatewayCloudNode); //Temporal change: priorities_quantity mapped to 5 for the current dataset
                            }

                            // stating the simulation

                            long startTime = System.currentTimeMillis();

                            System.out.println("Simulation running...");
                            Log.disable();
                            CloudSim.startSimulation();

                            TSPJobManager.releaseFinishedTasks(CloudSim.clock());

                            // showing the simulation results
                            Log.enable();
                            controller.print();


                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime;
                            double durationMinutes = duration / 60000.0;
                            System.out.println("Episode running time: " + durationMinutes + " minutes");

                            TSPSocketClient.saveModel(episode_number);
                            TSPSocketClient.nextEpisode();

                            // Dataset,Qty fog nodes,Strategy,Random seed,P1,P2,P3,P4,P5,Simulation time,Avg task time,Total energy,Gateway idle energy,Gateway busy energy,Gateway total energy
                            csvResultsWriterD.append(episode_number+","+
                                    dataset+","+fogNodesQuantity+","+strategy.name() + "," + randomSeed+","+
                                            TSPJobManager.getQuantityOfExceededDeadline(1)+","+TSPJobManager.getQuantityOfExceededDeadline(2)+","+TSPJobManager.getQuantityOfExceededDeadline(3)+","+TSPJobManager.getQuantityOfExceededDeadline(4)+","+TSPJobManager.getQuantityOfExceededDeadline(5)+","+
                                            controller.TotalExecutionTime+","+TSPJobManager.getTaskCompletionTimeAvg()+","+controller.TotalEnergy+","+
                                            TSPJobManager.getGatewayIdleEnergyConsumption()+","+TSPJobManager.getGatewayBusyEnergyConsumption()+","+(TSPJobManager.getGatewayIdleEnergyConsumption()+TSPJobManager.getGatewayBusyEnergyConsumption())+","+TSPJobManager.getAvgGatewayBusyTime()
                                            +","+TSPJobManager.getRewardHistoryAvg()
                                            +"\n"
                            );
                        }
                    }
                }
            }
        }

        csvResultsWriterD.flush();
        csvResultsWriterD.close();

        TSPSocketClient.closeConnection();
    }


    public static void main(String[] args) {
        try {
            runExplorationMode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TSPSocketClient.closeConnection();

    }
}