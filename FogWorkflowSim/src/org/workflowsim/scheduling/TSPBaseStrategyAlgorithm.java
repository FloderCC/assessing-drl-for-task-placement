package org.workflowsim.scheduling;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.TSPEnvHelper;
import org.workflowsim.utils.TSPJobManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base template for the TSP problem strategies
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
public abstract class TSPBaseStrategyAlgorithm extends BaseSchedulingAlgorithm {

    /**
     * This method will be not used in TSP
     */
    public void run() {
        return;
    }

    /**
     * This method will replace the 'run' method in TSP
     */

    public abstract double runSteep() throws Exception;

    protected boolean there_were_deadlines;
    public boolean getThereWereDeadlines(){
        return this.there_were_deadlines;
    }

    protected boolean all_severs_are_busy;
    public boolean getAllServersAreBusy(){
        return this.all_severs_are_busy;
    }

    /**
     * Return the list of devices available for placement
     * @return the list of fog and cloud devices
     */
    public List<Vm> getNotMobileVmList() {
        Predicate<CondorVM> byLayer = vm -> !vm.getHost().getDatacenter().getName().startsWith("m");
        return (List)getVmList().stream().filter(byLayer).collect(Collectors.toList());
    }

    public boolean thereAreServersInIdle(){
        for (Vm value : getNotMobileVmList()) {
            CondorVM vm = (CondorVM) value;
            if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                return true;
            }
        }
        return false;
    }

    public boolean theGatewayIsInIdle(){
        return getGatewayVm().getState() == WorkflowSimTags.VM_STATUS_IDLE;
    }

    /**
     * Return the simulated gateway device
     * @return the mobile device
     */
    public CondorVM getGatewayVm(){
        for (Iterator itc = getVmList().iterator(); itc.hasNext();) { //VM list
            CondorVM vm = (CondorVM) itc.next();
            if (vm.getHost().getDatacenter().getName().startsWith("m")){
                return vm;
            }
        }
        return null;
    }

    /**
     * NEW DRL REWARD
     * Compute the strategy's action reward
     * @param vm the vm for allocating the task
     * @param tsp_task the task to be allocated
     * @return the drl reward
     */
    public double getReward(TSPTask tsp_task, CondorVM vm, boolean deadline_exceeded, double task_decision_time, double task_running_time) {
        // Task parameters
        double priority = tsp_task.getPriority(); // Task priority (1 to 5)
        double arrivalTime = tsp_task.getArrivalTime(); // Task arrival time
        double deadline = tsp_task.getTimeDeadlineFinal(); // Task deadline

        // Penalty parameter
        double largePenalty = 1.0; // Large penalty for missed deadlines

        double maxTaskRunningTime = deadline - arrivalTime; // Maximum time to complete the task without considering the task_decision_time

        // Weight based on task priority (higher priorities are more important)
        double priorityWeight = priority / 5.0;

        // Total reward
        double totalReward;

        if (deadline_exceeded){
            // Scaled penalty
            totalReward = - largePenalty * priorityWeight * Math.min(1, task_running_time / maxTaskRunningTime);
        }else {
            // Scaled processing reward (penalizes longer completion times)
            totalReward = priorityWeight * (1 - task_running_time / maxTaskRunningTime);
        }

        // Save the reward for analysis or debugging
        TSPJobManager.saveReward(totalReward);


        return totalReward;
    }

}
