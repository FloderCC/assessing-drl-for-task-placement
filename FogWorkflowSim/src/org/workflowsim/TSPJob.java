package org.workflowsim;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 The Job concept in the TSP extension is far from the one used in WorkflowSim.
 In this case, it refers to the restrictions for the execution of the tasks that are
 defined in the jobs dataset. This class represents a job.
 *
 * @author Julio Corona
 * @since WorkflowSim Toolkit 1.0 's TSP extension
 */
public class TSPJob {
    /**
     * Maximum number of tasks can be executed simultaneously
     */
    private int max_parallel_executable_tasks;

    /**
     * Task list can be executed simultaneously
     */
    private ArrayList<ArrayList<Integer>> tasks_which_can_run_in_parallel;

    /**
     * List of tasks that are running at a certain time
     */
    private LinkedList<TSPTask> tasks_running;

    /**
     * Quantity of tasks that are running at a certain time
     */
    public int tasks_running_quantity;

    /**
     * Creates a new entity
     * @param max_parallel_executable_tasks Maximum number of tasks can be executed simultaneously
     * @param tasks_which_can_run_in_parallel Task list can be executed simultaneously
     */
    public TSPJob(int max_parallel_executable_tasks, ArrayList<ArrayList<Integer>> tasks_which_can_run_in_parallel) {
        this.max_parallel_executable_tasks = max_parallel_executable_tasks;
        this.tasks_which_can_run_in_parallel = tasks_which_can_run_in_parallel;
        this.tasks_running = new LinkedList<>();
        this.tasks_running_quantity = 0;
    }

    /**
     * Add a task to the list of running tasks
     * @param task the task to be added
     */
    public void addTasksRunning(TSPTask task) {
        this.tasks_running.add(task);
        this.tasks_running_quantity+=1;
    }

    /**
     * Remove a task to the list of running tasks
     * @param task the task to be removed
     */
    public void removeTasksRunning(TSPTask task) {
        this.tasks_running.remove(task);
        this.tasks_running_quantity-=1;
    }

    /**
     * It determines if a task can be executed or not taking into account the number of tasks that are being executed
     * and the parallelism restrictions between them.
     *
     * @param id the id of the task to check if it can be executed
     * @return true in the task can be executed, else false
     */
    public boolean canRunTask(int id) {
        if (this.tasks_running_quantity >= this.max_parallel_executable_tasks) {
            return false;
        }

        for (ArrayList<Integer> tasks: this.tasks_which_can_run_in_parallel) {
            if (tasks.contains(id)){
                for (TSPTask task_running: this.tasks_running) {
                    if (!tasks.contains(task_running.getTaskId())){
                        return false;
                    }
                }
                return true;
            }
        }

        return true;
    }
}
