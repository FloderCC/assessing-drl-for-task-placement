package org.workflowsim.utils;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.fog.entities.FogDevice;
import org.fog.utils.FogLinearPowerModel;
import org.workflowsim.Job;
import org.workflowsim.TSPJob;
import org.workflowsim.TSPTask;
import org.workflowsim.Task;

import java.util.*;
import java.util.regex.Pattern;

/**
 This class allows dealing with the task execution restrictions specified on the jobs
 *
 * @author Julio Corona
 * @since WorkflowSim Toolkit 1.0 's TSP extension
 */
public class TSPJobManager {

    /**
     * Clean the simulation auxiliary variables before each simulation
     */
    public static void initSimulationVariables(double myRealGatewayMIPS, double simulatedGatewayMIPS, List<FogDevice> fogDevices, int numEpisodes){

        //variables to be used in the simulation
        jobs = new HashMap<>();
        deadline_exceeded_quantity = 0;
        total_task_completion_time = 0;
        total_task_running_time = 0;
        quantity_task_completed = 0;
        executing_task = new ArrayList<>();
        last_executed_task_no = 0;
        device_host_busy_time = new HashMap<>();

        //variables to be used in the simulation
        gateway_idle_energy_consumption = -1;
        gateway_busy_energy_consumption = -1;
        my_real_gateway_mips = myRealGatewayMIPS;
        simulated_gateway_mips = simulatedGatewayMIPS;

        // init the device's busy time list
        for (FogDevice fogDevice: fogDevices) {
            for (Host host: fogDevice.getHostList()) {
                device_host_busy_time.put(host.getId(), 0.0);
            }
        }
        num_episodes = numEpisodes;

        deadline_exceeded_ids = new HashMap<>();
        deadline_exceeded = new HashMap<>();
        task_completion_time_history = new HashMap<>();
        reward_history = new HashMap<>();
        gatewayBusyTimes = new HashMap<>();

        for(int i = 0; i < num_episodes; i++) {
            deadline_exceeded_ids.put(i, new ArrayList<>());
            task_completion_time_history.put(i, new HashMap<>());
            reward_history.put(i, new ArrayList<>());
            gatewayBusyTimes.put(i, new ArrayList<>());


            deadline_exceeded.put(i, new HashMap<>());
            // staring in 0 for each priority
            for (int j = 1; j <= 5; j++) {
                deadline_exceeded.get(i).put(j, 0);
            }
        }
    }

    public static int getNumberOfTasksPerEpisode() {
        return number_of_tasks_per_episode;
    }

    private static int current_episode;

    public static void setEpisodeNumber(int episode_no){
        current_episode = episode_no;
    }

    /**
     * Dictionary to store the execution status of each job
     */
    private static Map<Integer, TSPJob> jobs;

    /**
     * Dictionary to store the quantity of task exceeding the deadline
     */
    private static Map<Integer, Map<Integer, Integer>> deadline_exceeded; // episode, priority, quantity

    private static Map<Integer, ArrayList<Integer>> deadline_exceeded_ids; // episode, tasks_ids

    public static ArrayList<Integer> getDeadlineExceededIds() {
        ArrayList<Integer> allExceededIds = new ArrayList<>();
        for (ArrayList<Integer> ids : deadline_exceeded_ids.values()) {
            allExceededIds.addAll(ids);
        }
        return allExceededIds;
    }

    public static ArrayList<Integer> getDeadlineExceededIds(int episode) {
        return deadline_exceeded_ids.get(episode);
    }


    /**
     * Auxiliary variable for storing the sum of the tasks' completion time
    */

    private static double total_task_completion_time;

    /**
     * Auxiliary variable for storing the sum of the tasks' completion time including the decision time
    */

    public static HashMap<Integer, Double> getTaskCompletionTimeHistory() {
        //returning the task_completion_time_history regardless of the episode
        HashMap<Integer, Double> allTaskCompletionTimeHistory = new HashMap<>();
        for (HashMap<Integer, Double> episodeTaskCompletionTimeHistory : task_completion_time_history.values()) {
            allTaskCompletionTimeHistory.putAll(episodeTaskCompletionTimeHistory);
        }
        return allTaskCompletionTimeHistory;
    }

    public static HashMap<Integer, Double> getTaskCompletionTimeHistory(int episode) {
        return task_completion_time_history.get(episode);
    }

    public static double getTaskCompletionTimeAvg() {
        HashMap<Integer, Double> all_task_completion_time = getTaskCompletionTimeHistory();
        if (all_task_completion_time.isEmpty()) {
            return 0.0; // Or handle empty list appropriately
        }

        double sum = 0;
        int qty = 0;
        for (double completionTime : all_task_completion_time.values()) {
            sum += completionTime;
            qty++;
        }

        return sum / qty;
    }

    public static double getTaskCompletionTimeAvg(int episode){
        HashMap<Integer, Double> episode_task_completion_time = task_completion_time_history.get(episode);
        if (episode_task_completion_time.isEmpty()) {
            return 0.0; // Or handle empty list appropriately
        }

        double sum = 0;
        for (double completionTime : episode_task_completion_time.values()) {
            sum += completionTime;
        }

        return sum / episode_task_completion_time.size();
    }

    /**
     * Auxiliary variable for storing the tasks' completion time average
    */

    private static HashMap<Integer, HashMap<Integer, Double>> task_completion_time_history; // episode, task_id, completion_time

    /**
     * Auxiliary variable for storing the rewards
     */

    private static HashMap<Integer, ArrayList<Double>> reward_history; // episode, rewards

    public static void saveReward(double reward) {
        reward_history.get(current_episode).add(reward);
    }

    public static ArrayList<Double> getTaskRewardHistory() {
        ArrayList<Double> allRewards = new ArrayList<>();
        for (ArrayList<Double> rewards : reward_history.values()) {
            allRewards.addAll(rewards);
        }
        return allRewards;
    }

    public static ArrayList<Double> getTaskRewardHistory(int episode) {
        return reward_history.get(episode);
    }

    public static double getRewardHistoryAvg(){
        double sum = 0.0;

        ArrayList<Double> task_reward_history = getTaskRewardHistory();

        for (double reward : task_reward_history) {
            sum += reward;
        }
        return reward_history.isEmpty() ? 0.0 : sum / task_reward_history.size();
    }

    public static double getRewardHistoryAvg(int episode){
        double sum = 0.0;
        ArrayList<Double> task_reward_history = getTaskRewardHistory(episode);
        for (double reward : task_reward_history) {
            sum += reward;
        }
        return reward_history.isEmpty() ? 0.0 : sum / task_reward_history.size();
    }

    /**
     * Auxiliary variable for storing the sum of the tasks' running time
    */

    private static double total_task_running_time;

    /**
     * Auxiliary variable for storing quantity of tasks' completed
    */

    private static double quantity_task_completed;

    /**
     * Create a new job
     * @param job_id the job id
     * @param max_parallel_executable_tasks the maximum number of tasks can be executed simultaneously in this job
     * @param tasks_which_can_run_in_parallel the task list can be executed simultaneously in this job
     */

    public static void createTSPJob(Integer job_id, String max_parallel_executable_tasks,  String tasks_which_can_run_in_parallel){
        String[] task_id_list = tasks_which_can_run_in_parallel.substring(2, tasks_which_can_run_in_parallel.length() - 2).split(Pattern.quote("],["));

        ArrayList<ArrayList<Integer>> tasks_which_can_run_in_parallel_parsed = new ArrayList<ArrayList<Integer>>(task_id_list.length);
        for (int i=0; i<task_id_list.length; i++){
            String[] task = task_id_list[i].split(",");
            ArrayList<Integer> tasks_parsed = new ArrayList<Integer>(task.length);

            for (int j=0; j<task.length; j++){
                tasks_parsed.add(Integer.parseInt(task[j]));
            }
            tasks_which_can_run_in_parallel_parsed.add(tasks_parsed);
        }

        //creating the job
        TSPJob tsp_job = new TSPJob(Integer.parseInt(max_parallel_executable_tasks), tasks_which_can_run_in_parallel_parsed);

        jobs.put(job_id, tsp_job);
    }

    /**
     * Check if a task can be executed
     * @param job_id the task's job id
     * @param task_id the task id
     * @return true if it can be executed, false otherwise
     */
    public static boolean canRunTask(Integer job_id, Integer task_id){
        return jobs.get(job_id).canRunTask(task_id);
    }


    /**
     * List of all running tasks
     */
    private static ArrayList<TSPTask> executing_task;

    /**
     * Add a task to the list of running tasks
     * @param task the task to be added
     */

    public static void addTaskRunning(Cloudlet cloudlet, TSPTask task, double decision_time, double task_start_execution_timestamp){
        //restringing the execution for considering scheduling restrictions
        jobs.get(task.getJobId()).addTasksRunning(task);

        //restringing the execution for the end of the scheduling restrictions
        task.setDecisionTime(decision_time);
        cloudlet.setExecStartTime(task_start_execution_timestamp);
        task.setTimeStartProcessing(task_start_execution_timestamp);

        executing_task.add(task);
    }

    /**
     * Review the list of tasks that have already finished and delete them from the list of tasks in progress
     * @param time the simulation time
     */
    public static void releaseFinishedTasks(double time){
        Stack<TSPTask> finished_tasks = new Stack<TSPTask>();

        for (TSPTask task: executing_task) {
            if (task.getTaskFinishTime() != -1 && task.getTaskFinishTime() >= time){
                jobs.get(task.getJobId()).removeTasksRunning(task);
                finished_tasks.push(task);

                total_task_completion_time += task.getTaskFinishTime() - task.getArrivalTime();

                total_task_running_time += task.getTaskFinishTime() - task.getTimeStartProcessing();
                quantity_task_completed += 1;

                task_completion_time_history.get(current_episode).put(task.getCloudletId(), task.getTaskFinishTime() - task.getArrivalTime() + task.getDecisionTime());

            }
        }

        while (!finished_tasks.isEmpty()){
            executing_task.remove(finished_tasks.pop());
        }
    }

    /**
     * Returns the average time to complete the tasks
     */

    static public double getAvgTaskCompletionTime(){
        return total_task_completion_time / quantity_task_completed;
    }

    /**
     * Returns the average time to complete the tasks, when the last task is going to be assigned
     */

    static public double getAvgTaskCompletionTime(double task_completion_time){
        return (total_task_completion_time + task_completion_time) / (quantity_task_completed + 1);
    }

    /**
     * Returns the average time to run the tasks
     */

    static public double getAvgTaskRunningTime(){
        return total_task_running_time / quantity_task_completed;
    }

    /**
     * Returns the average time to run the tasks, when the last task is going to be assigned
     */

    static public double getAvgTaskRunningTime(double task_running_time){
        return (total_task_running_time + task_running_time) / (quantity_task_completed + 1);
    }

    /**
     * Returns the next time when one of the tasks that are running will end
     */
    private static double getNextFinishTime(){
        double next_finish_time = Double.MAX_VALUE;
        if (executing_task.size() > 0){
            for (int i=0; i < executing_task.size(); i++){
                if (executing_task.get(i).getTaskFinishTime() != -1 &&  executing_task.get(i).getTaskFinishTime() < next_finish_time){
                    next_finish_time = executing_task.get(i).getTaskFinishTime();
                }
            }
            return next_finish_time;
        }
        return next_finish_time;
    }

    /**
     * Returns the next list of jobs that will be ready to be executed at a given clock time
     * @param cloudletList The list of jobs pending to be executed
     * @param clock the clock time
     * @return the list of jobs ready to be executed
     */
    private static ArrayList<Job> getAvailableJobs(List cloudletList, double clock){
        ArrayList<Job> jobs = new ArrayList<>();

        for (int i=0; i < cloudletList.size(); i++){
            Job job = (Job)cloudletList.get(i);
            TSPTask tsp_task = (TSPTask)job.getTaskList().get(0);
            if (tsp_task.getArrivalTime() <= clock  && (!Parameters.getConsiderTasksParallelismRestrictions() || TSPJobManager.canRunTask(tsp_task.getJobId(), tsp_task.getTaskId()))){
                jobs.add((Job)cloudletList.get(i));
            }
        }

        return jobs;
    }

    /**
     * Returns the next time when one of the pending jobs will arrive
     * @param cloudletList the list of pending jobs
     */
    private static double getFutureJobsTime(List cloudletList){
        double time = Double.MAX_VALUE;

        for (int i=0; i < cloudletList.size(); i++){
            Job job = (Job)cloudletList.get(i);
            TSPTask tsp_task = (TSPTask)job.getTaskList().get(0);
            if (tsp_task.getArrivalTime() <= time  && (!Parameters.getConsiderTasksParallelismRestrictions() || TSPJobManager.canRunTask(tsp_task.getJobId(), tsp_task.getTaskId()))){
                time = tsp_task.getArrivalTime();
            }
        }
        return time;
    }

    /**
     * Returns the next jobs that will be able to be executed either due to their submission time or due to simulation
     * restrictions
     * @param cloudletList the list of pending jobs
     * @param clock the clock time
     * @return the time of the next availability and the list of available jobs
     */
    public static Object[] getNextAvailableJobs(List cloudletList, double clock){
        ArrayList<Job> next_available_jobs_to_income = getAvailableJobs(cloudletList, clock);

        if (clock == Double.MAX_VALUE){
            return new Object[]{clock, new ArrayList<>()};
        }

        if (!next_available_jobs_to_income.isEmpty()){
            return new Object[]{clock, next_available_jobs_to_income};
        }

        double next_finish_time =  getNextFinishTime();
        double future_jobs_time = getFutureJobsTime(cloudletList);

        double new_time = Math.min(next_finish_time, future_jobs_time);

        releaseFinishedTasks(new_time);

        return getNextAvailableJobs(cloudletList, new_time);
    }

    /**
     * Count the tasks that exceeded its deadline
     * @param tsp_task the task
     */

    private static int number_of_tasks_per_episode;

    public static void setNumberOfTasksPerEpisode(int number_of_tasks_per_episode) {
    	TSPJobManager.number_of_tasks_per_episode = number_of_tasks_per_episode;
    }

    public static int getTaskEpisode(Task tsp_task){
        return tsp_task.getCloudletId() / number_of_tasks_per_episode + 1;
    }

    public static void registerTaskExceedingDeadline(TSPTask tsp_task){

        int taskPriority = tsp_task.getPriority();

        deadline_exceeded_quantity+=1;
        if (deadline_exceeded.get(current_episode).containsKey(taskPriority)){
            deadline_exceeded.get(current_episode).put(taskPriority, deadline_exceeded.get(current_episode).get(taskPriority) + 1);
        }else
        {
            deadline_exceeded.get(current_episode).put(taskPriority, 1);
        }

        deadline_exceeded_ids.get(current_episode).add(tsp_task.getCloudletId());
    }

    public static void printTaskExceededDeadlineQuantities(){
        Log.printLine("Exceeded deadlines quantity:");
        // printing the total of tasks that exceeded the deadline regardless of the episode
        Log.printLine("Total: " + deadline_exceeded_quantity);
    }

    public static void printTaskExceededDeadlineQuantitiesByPriority(){
        Log.printLine("Exceeded deadlines by priorities:");
        for (int i=1; i<=5; i++){
            Log.printLine("Priority " + i + ": " + getQuantityOfExceededDeadline(i));
        }
    }

    public static int getQuantityOfExceededDeadline(int priority){

        int number_of_tasks = 0;
        for (Integer episode: deadline_exceeded.keySet()) {
            if (deadline_exceeded.get(episode).containsKey(priority)){
                number_of_tasks += deadline_exceeded.get(episode).get(priority);
            }
        }
        return number_of_tasks;
    }

    public static int getQuantityOfExceededDeadline(int episode, int priority){
        return deadline_exceeded.get(episode).get(priority);
    }

    /**
     * Auxiliary attribute for know the last executed task
     */
    public static int last_executed_task_no;


    /**
     * Auxiliary attribute for know the quantity of tasks that the deadline was exceeded
     */
    private static int deadline_exceeded_quantity;


    public static int getTaskExceedingDeadlineQuantity(){
        return deadline_exceeded_quantity;
    }

    /**
     * List of device's busy time
     */
    private static Map<Integer, Double> device_host_busy_time;

    private static int num_episodes;

    public static int getNumEpisodes(){
        return num_episodes;
    }

    /**
     * Update the device's busy time list
     * @param host_id the device's host id
     * @param time the task execution time
     */
    public static void updateDeviceBusyTime(int host_id, double time){
        device_host_busy_time.replace(host_id, device_host_busy_time.get(host_id) + time);
    }

    public static double getEnergyConsumption(List<Vm> vmList){
        double energy = 0;
        for (Vm vm: vmList){
            double busy_time = device_host_busy_time.get(vm.getHost().getId());
            PowerHost host = (PowerHost)vm.getHost();
            FogLinearPowerModel powerModel = (FogLinearPowerModel) host.getPowerModel();
            energy += busy_time * powerModel.getPower(vm.getMips()/host.getTotalMips());

            double idle_time = CloudSim.clock() - busy_time;
            energy += idle_time * powerModel.getStaticPower();
        }

        return energy;
    }

    /**
     * Auxiliary var to storing the Gateway utilization for its final consumption
     */
    private static HashMap<Integer, ArrayList<Double[]>> gatewayBusyTimes;

    public static void registerGatewayBusyTimes(double timestamp, double computation_time, double cpu_percent){
        gatewayBusyTimes.get(current_episode).add(new Double[]{timestamp, computation_time, cpu_percent});
    }

    public static ArrayList<Double[]> getGatewayBusyTimes(){
        //get all gateway busy times regardless the episode
        ArrayList<Double[]> allGatewayBusyTimes = new ArrayList<>();
        for (ArrayList<Double[]> busy_time_in_episode: gatewayBusyTimes.values()) {
            allGatewayBusyTimes.addAll(busy_time_in_episode);
        }
        return allGatewayBusyTimes;
    }

    public static double getAvgGatewayBusyTime(){
        double sum = 0.0;
        int qty = 0;

        // get the busy time regardless the episode
        for (ArrayList<Double[]> busy_time_in_episode: gatewayBusyTimes.values()) {
            for (Double[] busy_time: busy_time_in_episode) {
                sum += busy_time[1];
                qty += 1;
            }
        }

        return gatewayBusyTimes.isEmpty() ? 0.0 : sum / qty;
    }

    public static double getAvgGatewayBusyTime(int episode){
        double sum = 0.0;
        for (Double[] busy_time: gatewayBusyTimes.get(episode)) {
            sum += busy_time[1];
        }
        return gatewayBusyTimes.isEmpty() ? 0.0 : sum / gatewayBusyTimes.get(episode).size();
    }


    private static double gateway_idle_energy_consumption;
    private static double gateway_busy_energy_consumption;

    private static FogLinearPowerModel gateway_power_model;

    public static double getGatewayTotalEnergyConsumption(double simulationFinalClock, PowerHost host){
        gateway_busy_energy_consumption = 0;
        double busy_time = 0;

        gateway_power_model = (FogLinearPowerModel) host.getPowerModel();

        for (Double[] busyTime: getGatewayBusyTimes()){
            double time = busyTime[1];
            double cpu_percent = busyTime[2];

            busy_time+=time;
            gateway_busy_energy_consumption += time * gateway_power_model.getPower(cpu_percent/100);
        }

        double idle_time = simulationFinalClock - busy_time;

        gateway_idle_energy_consumption = idle_time * gateway_power_model.getStaticPower();

        return gateway_busy_energy_consumption + gateway_idle_energy_consumption;
    }



    public static ArrayList<Double> getGatewayEnergyConsumptionHistory() {
        ArrayList<Double> gateway_energy_consumption_history = new ArrayList<>();

        double gateway_busy_energy_consumption = 0;
        double busy_time = 0;

        for (Double[] busyTime: getGatewayBusyTimes()){
            double time = busyTime[1];
            double cpu_percent = busyTime[2];

            double total_idle_time = busyTime[0] - busy_time;

            busy_time+=time;
            gateway_busy_energy_consumption += time * gateway_power_model.getPower(cpu_percent/100);

            double total_gateway_idle_energy_consumption = total_idle_time * gateway_power_model.getStaticPower();

            gateway_energy_consumption_history.add(gateway_busy_energy_consumption + total_gateway_idle_energy_consumption);
        }

        return gateway_energy_consumption_history;
    }

    public static ArrayList<Double> getGatewayEnergyConsumptionHistory(int episode) {
        ArrayList<Double> gateway_energy_consumption_history = new ArrayList<>();

        double gateway_busy_energy_consumption = 0;
        double busy_time = 0;

        for (Double[] busyTime: gatewayBusyTimes.get(episode)){
            double time = busyTime[1];
            double cpu_percent = busyTime[2];

            double total_idle_time = busyTime[0] - busy_time;

            busy_time+=time;
            gateway_busy_energy_consumption += time * gateway_power_model.getPower(cpu_percent/100);

            double total_gateway_idle_energy_consumption = total_idle_time * gateway_power_model.getStaticPower();

            gateway_energy_consumption_history.add(gateway_busy_energy_consumption + total_gateway_idle_energy_consumption);
        }

        return gateway_energy_consumption_history;
    }

    public static double getGatewayIdleEnergyConsumption() {
        return gateway_idle_energy_consumption;
    }

    public static double getGatewayIdleEnergyConsumption(int episode) {
        double gateway_idle_energy_consumption = 0;
        double busy_time = 0;

        for (Double[] busyTime: gatewayBusyTimes.get(episode)){
            double time = busyTime[1];

            double total_idle_time = busyTime[0] - busy_time;

            busy_time+=time;
            double total_gateway_idle_energy_consumption = total_idle_time * gateway_power_model.getStaticPower();

            gateway_idle_energy_consumption += total_gateway_idle_energy_consumption;
        }

        return gateway_idle_energy_consumption;
    }

    public static double getGatewayBusyEnergyConsumption() {
        return gateway_busy_energy_consumption;
    }

    public static double getGatewayBusyEnergyConsumption(int episode) {
        double gateway_busy_energy_consumption = 0;

        for (Double[] busyTime: gatewayBusyTimes.get(episode)){
            double time = busyTime[1];
            double cpu_percent = busyTime[2];
            gateway_busy_energy_consumption += time * gateway_power_model.getPower(cpu_percent/100);
        }

        return gateway_busy_energy_consumption;
    }

    private static double my_real_gateway_mips;
    private static double simulated_gateway_mips;

    public static double parseComputationTime(double computation_time) {
        return computation_time * my_real_gateway_mips / simulated_gateway_mips;
    }
}
