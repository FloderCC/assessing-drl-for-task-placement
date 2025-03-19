package org.workflowsim.scheduling;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.utils.TSPDecisionResult;
import org.workflowsim.utils.TSPEnvHelper;
import org.workflowsim.utils.TSPJobManager;
import org.workflowsim.utils.TSPSocketClient;
import java.util.List;

/**
 * Strategy method for task scheduling and placement
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
public class TSPSchedulingAndPlacementAlgorithm extends TSPBaseStrategyAlgorithm {


    public TSPSchedulingAndPlacementAlgorithm()
    {
        super();
    }


    public double runSteep(){

        TSPJobManager.releaseFinishedTasks(CloudSim.clock());

        return schedulerAndPlacer();
    }
    public double schedulerAndPlacer() {
        List cloudletList = getCloudletList();
        List vmList = getNotMobileVmList();

        Double[] state = TSPEnvHelper.parseStateWithTasksAndEnv(cloudletList, vmList, CloudSim.clock());

        TSPDecisionResult response = TSPSocketClient.askForDecisionWithActionId(TSPJobManager.last_executed_task_no, state);

        int[] action = response.getAction();
        double decision_time = response.getTime();

        if (action[0] == -1) { // means there was no server with enough resources for any task
            all_severs_are_busy = true;
            return decision_time;
        }
        all_severs_are_busy = false;

        Cloudlet cloudlet = (Cloudlet) cloudletList.get(action[0]);
        TSPTask tsp_task = (TSPTask) cloudlet;
        CondorVM vm = (CondorVM) vmList.get(action[1]);

        double task_offloading_time = TSPEnvHelper.getOffloadingTimeByFogDeviceId(vm.getHost().getDatacenter().getId(), tsp_task.getStorage());
        double task_running_time = tsp_task.getMi() / vm.getMips();
        boolean deadline_exceeded = CloudSim.clock() + decision_time + task_offloading_time + task_running_time > tsp_task.getTimeDeadlineFinal();
        double reward = getReward(tsp_task, vm, deadline_exceeded, decision_time, task_running_time);

        if (deadline_exceeded) {
            TSPJobManager.registerTaskExceedingDeadline(tsp_task);
            this.there_were_deadlines = true;
            cloudletList.remove(action[0]);
        }else {
            this.there_were_deadlines = false;
            vm.setState(WorkflowSimTags.VM_STATUS_BUSY);
            cloudlet.setVmId(vm.getId());
            getScheduledList().add(cloudlet);
            TSPJobManager.addTaskRunning(cloudlet, tsp_task, decision_time, CloudSim.clock() + decision_time + task_offloading_time);
            TSPJobManager.updateDeviceBusyTime(vm.getHost().getId(), task_running_time);
        }

        TSPSocketClient.saveReward(TSPJobManager.last_executed_task_no, reward);
        if (TSPJobManager.last_executed_task_no != 0){
            //updating the placer information
            TSPSocketClient.retrain(TSPJobManager.last_executed_task_no - 1, state);
        }

        TSPJobManager.last_executed_task_no += 1;

        return decision_time;
    }

}
