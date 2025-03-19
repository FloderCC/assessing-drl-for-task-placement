package org.workflowsim.utils;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import java.util.List;

/**
 This class helps to represent the environment information for Reinforcement Learning agents
 *
 * @author Julio Corona
 * @since WorkflowSim Toolkit 1.0 's TSP extension
 */
public class TSPEnvHelper {
    /**
     * Return one array containing the environment information regarding task, fog and cloud servers
     * format its: [T1.MI, T.RAM, T.Storage, T.ArrivalTime, T.TimeDeadlineFinal, T.Priority, Timestamp
     * S1.MIPS, S1.RAM, S1.Storage, S1.Status, ... , Sn.Mips, Sn.RAM, Sn.Storage, Sn.Status] where T refers to the task to be placed as Sn the server n
     * @return array with the environment information
     */

    public static Double[] parseStateWithTaskAndEnv(TSPTask task, List notMobileVmList, double cloudSimClock){
        Double[] state = new Double[7 + notMobileVmList.size() * 4];

        // current task info
        state[0] = 1.0 * task.getMi();
        state[1] = 1.0 * task.getRam();
        state[2] = 1.0 * task.getStorage();
        state[3] = 1.0 * task.getArrivalTime();
        state[4] = 1.0 * task.getTimeDeadlineFinal();
        state[5] = 1.0 * task.getPriority();

        // global time
        state[6] = cloudSimClock;

        // Info regarding the nodes (is_idle, available_ram, available_storage, mpis)
        for (int i=0; i < notMobileVmList.size(); i++) {
            CondorVM vm = (CondorVM) notMobileVmList.get(i);
            int d = 7 + i * 4;
            state[d] = vm.getMips();
            state[d + 1] = 1.0 * vm.getRam();
            state[d + 2] = 1.0 * vm.getSize();

            if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                state[d + 3] = 0.0;
            } else {
                state[d + 3] = 1.0;
            }
        }

        return state;
    }

    /**
     * Return one array containing the environment information regarding task, fog and cloud servers
     * format its: [T1.MI, T1.RAM, T1.Storage, T1.ArrivalTime, T1.TimeDeadlineFinal, T1.Priority, ...
     * Tm.MI, Tm.RAM, Tm.Storage, Tm.ArrivalTime, Tm.TimeDeadlineFinal, Tm.Priority, Timestamp
     * S1.MIPS, S1.RAM, S1.Storage, S1.Status, ... , Sn.Mips, Sn.RAM, Sn.Storage, Sn.Status] where Tm refers to the task m, and Sn denotes the server n
     * @return array with the environment information
     */

    public static Double[] parseStateWithTasksAndEnv(List cloudletList, List notMobileVmList, double cloudSimClock){
        List<TSPTask> tasks = (List<TSPTask>) cloudletList;
        Double[] state = new Double[6 * tasks.size() + 1 + notMobileVmList.size() * 4];

        for (int i=0; i < tasks.size(); i++){
            TSPTask tsp_task = tasks.get(i);
            state[i * 6] = 1.0 * tsp_task.getMi();
            state[i * 6 + 1] = 1.0 * tsp_task.getRam();
            state[i * 6 + 2] = 1.0 * tsp_task.getStorage();
            state[i * 6 + 3] = 1.0 * tsp_task.getArrivalTime();
            state[i * 6 + 4] = 1.0 * tsp_task.getTimeDeadlineFinal();
            state[i * 6 + 5] = 1.0 * tsp_task.getPriority();
        }

        state[6 * tasks.size()] = cloudSimClock;

        for (int i=0; i < notMobileVmList.size(); i++) {
            CondorVM vm = (CondorVM) notMobileVmList.get(i);
            int d = 6 * tasks.size() + 1 + i * 4;
            state[d] = vm.getMips();
            state[d + 1] = 1.0 * vm.getRam();
            state[d + 2] = 1.0 * vm.getSize();

            if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                state[d + 3] = 0.0;
            } else {
                state[d + 3] = 1.0;
            }
        }

        return state;
    }

    /**
     * Parse a string to an array of int
     * @param str the string to be parsed
     * @return the resulting array of int
     */
    public static int[] parseStrArrayToIntArray(String str){
        String[] string = str.split(",");

        int size = string.length;
        int [] arr = new int [size];
        for(int i=0; i<size; i++) {
            arr[i] = Integer.parseInt(string[i]);
        }
        return arr;
    }

    /**
     * Parse a string to a float an array of int
     * @param str the string to be parsed
     * @return the resulting array of int
     */
    public static TSPDecisionResult parseStrArrayToTSPDecisionResult(String str){
        String[] string = str.split("d");
        double computation_time;

        //parsing the computation cost
        String[] computing_cost = string[0].split(",");

        if (Parameters.getConsiderGatewayComputationTime()){
            computation_time = Double.parseDouble(computing_cost[0]);
        }else {
            computation_time = 0;
        }

        computation_time = TSPJobManager.parseComputationTime(computation_time);

        TSPJobManager.registerGatewayBusyTimes(CloudSim.clock(), computation_time, Double.parseDouble(computing_cost[1]));

        //parsing the decision result
        string = string[1].split(",");

        int size = string.length;
        int [] arr = new int [size];
        for(int i=0; i<size; i++) {
            arr[i] = Integer.parseInt(string[i].trim());
        }
        return new TSPDecisionResult(computation_time, arr);
    }

    private static double fog_latency;
    private static double fog_upload_bandwidth_MB;
    private static double cloud_latency;
    private static double cloud_upload_bandwidth_MB;

    public static void setUploadRateVariables(double f_latency, double f_upload_bandwidth_Mb, Double c_latency, Double c_upload_bandwidth_Mb){
        fog_latency=f_latency;
        fog_upload_bandwidth_MB=f_upload_bandwidth_Mb / 8;

        if (c_latency != null){
            cloud_latency=c_latency;
        }

        if (c_upload_bandwidth_Mb != null){
            cloud_upload_bandwidth_MB=c_upload_bandwidth_Mb / 8;
        }

    }

    private static int cloud_fog_device_id;

    public static void setCloudId(Integer cloudFDid){
        if (cloudFDid != null){
            cloud_fog_device_id = cloudFDid;
        }
    }

    public static double getOffloadingTimeByFogDeviceId(int FDid, double taskSize){
        if (FDid == cloud_fog_device_id) {
            return cloud_latency + taskSize / cloud_upload_bandwidth_MB;
        }
        return fog_latency + taskSize / fog_upload_bandwidth_MB;
    }

}
