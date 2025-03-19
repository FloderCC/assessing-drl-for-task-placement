package org.workflowsim.scheduling;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.utils.*;

import java.util.List;

/**
 * Strategy method for task placing
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
public class TSPPlacementAlgorithm extends TSPBaseStrategyAlgorithm {


    public TSPPlacementAlgorithm()
    {
        super();
    }

    public double runSteep(){

        List cloudletList = getCloudletList();

        TSPJobManager.releaseFinishedTasks(CloudSim.clock());

        Cloudlet cloudlet = (Cloudlet) cloudletList.get(0);
        TSPTask tsp_task = ((TSPTask) cloudlet);

        double decision_time = placer(cloudlet, tsp_task);

//        if (the_task_was_submitted_or_dropped){
//            cloudletList.remove(0);
//        }

        if (this.there_were_deadlines){
            cloudletList.remove(0);
        }

        return decision_time;
    }

//    private boolean the_task_was_submitted_or_dropped = false;

    public double placer(Cloudlet cloudlet, TSPTask tsp_task){

        //list of fog and cloud devices
        List<Vm> not_mobile_vm_list = getNotMobileVmList();

        //parses the task and the device status to a vector
        Double[] state = TSPEnvHelper.parseStateWithTaskAndEnv(tsp_task, not_mobile_vm_list, CloudSim.clock());

        //call the placement agent
        TSPDecisionResult response = TSPSocketClient.askForDecisionWithActionId(TSPJobManager.last_executed_task_no, state);

        int action = response.getAction()[0];

        double decision_time = response.getTime();

        if (action == -1){
            all_severs_are_busy = true;
            return decision_time;
        }
        all_severs_are_busy = false;

        CondorVM vm = (CondorVM) not_mobile_vm_list.get(action);


        double task_offloading_time = TSPEnvHelper.getOffloadingTimeByFogDeviceId(vm.getHost().getDatacenter().getId(), tsp_task.getStorage());

//        double task_decision_and_offloading_time = decision_time + task_offloading_time;

//        double task_start_execution_timestamp = CloudSim.clock() + task_decision_and_offloading_time;
        double task_running_time = tsp_task.getMi() / vm.getMips();

        boolean deadline_exceeded = CloudSim.clock() + decision_time + task_offloading_time + task_running_time > tsp_task.getTimeDeadlineFinal();

        double reward = getReward(tsp_task, vm, deadline_exceeded, decision_time, task_running_time);

        if (deadline_exceeded) {
            TSPJobManager.registerTaskExceedingDeadline(tsp_task);
            this.there_were_deadlines = true;
            cloudlet.setVmId(-1);

        }else {
            this.there_were_deadlines = false;
            vm.setState(WorkflowSimTags.VM_STATUS_BUSY);
            cloudlet.setVmId(vm.getId());
            getScheduledList().add(cloudlet);
            TSPJobManager.addTaskRunning(cloudlet, tsp_task, decision_time, CloudSim.clock() + decision_time + task_offloading_time);
            TSPJobManager.updateDeviceBusyTime(vm.getHost().getId(), task_running_time);
        }

//        double reward = getReward(tsp_task, vm, deadline_exceeded, decision_time, task_start_execution_timestamp + task_running_time - tsp_task.getArrivalTime());
//        double reward = getReward(tsp_task, vm, deadline_exceeded, decision_time, task_running_time);


        TSPSocketClient.saveReward(TSPJobManager.last_executed_task_no, reward);
        if (TSPJobManager.last_executed_task_no != 0){
            //updating the placer information
            TSPSocketClient.retrain(TSPJobManager.last_executed_task_no - 1, state);
        }

        TSPJobManager.last_executed_task_no += 1;


//        the_task_was_submitted_or_dropped = true;

        return decision_time;
    }

}
