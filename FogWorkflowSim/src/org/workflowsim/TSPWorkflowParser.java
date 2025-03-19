/**
 * This class extends from the WorkflowParser class for TSP problems
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
package org.workflowsim;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.TSPEnvHelper;
import org.workflowsim.utils.TSPJobManager;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Since the WorkflowParser is defined as "final" this class replaces the WorkflowParser class for TSP problems
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
public final class TSPWorkflowParser {

    private List<Task> taskList;
    /**
     * User id. used to create a new task.
     */
    private final int userId;

    public List<Task> getTaskList() {
        return taskList;
    }

    /**
     * Sets the task list
     *
     * @param taskList the task list
     */
    protected void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    /**
     * Map from task name to task.
     */
    protected Map<String, Task> mName2Task;

    /**
     * Initialize a WorkflowParser
     *
     * @param userId the brokerId id
     * mode
     */
    public TSPWorkflowParser(int userId) {
        this.userId = userId;
        this.mName2Task = new HashMap<>();
        setTaskList(new ArrayList<>());
    }

    /**
     * Start to parse a workflow which is a xml file(s).
     */
    public void parse() {
        parseCsvFile(Parameters.getDaxPath());
    }

    /**
     * TSP modification: This method replaces the "parseXmlFile" method to read the tasks
     * @param path the path where are the job.csv and tasks.csv files
     */
    public void parseCsvFile(String path){
        int episodeNo = Parameters.getEpisodeNumber();
        TSPJobManager.setEpisodeNumber(episodeNo);
        System.out.println("Loading dataset for episode: " + episodeNo);
        int taskId;
        try {
            if (episodeNo == 0){
                taskId=0;
            }else{
                taskId= TSPJobManager.getNumberOfTasksPerEpisode() * episodeNo;
            }

//            long last_task_submission = 0;
            long last_job_submission = 0;

            CSVReader job_reader = new CSVReader(new FileReader(path + "/Jobs.csv"));
            CSVReader task_reader = new CSVReader(new FileReader(path + "/Tasks.csv"));

            //skip the headers
            job_reader.readNext();
            task_reader.readNext();


            //reading job information
            String[] job_info;


            while ((job_info = job_reader.readNext()) != null) {

                int job_id = Integer.parseInt(job_info[1]);

                //creating the TSPJob
                TSPJobManager.createTSPJob(job_id, job_info[19], job_info[21]);

                //reading job's tasks
                String[] task_info = task_reader.peek();

                // vew validation because duplicated jobs
                if (task_info == null){
                    break;
                }

                int task_job_id = Integer.parseInt(task_info[2]);

                last_job_submission = last_job_submission + Long.parseLong(job_info[5]);

                while (task_job_id == job_id){

                    task_job_id = Integer.parseInt(task_info[2]);
                    int task_id = Integer.parseInt(task_info[1]);
                    long mi = Long.parseLong(task_info[3]) * (long)Parameters.getRuntimeScale();
                    long ram = Long.parseLong(task_info[4]);
                    long storage = Long.parseLong(task_info[5]);


                    long task_time_submission = last_job_submission + Long.parseLong(task_info[7]);

                    long time_deadline_final = task_time_submission + Long.parseLong(task_info[9]);

                    int priority_no = Integer.parseInt(task_info[17]);

                    //task creation
                    TSPTask task = new TSPTask(taskId, task_job_id, task_id, mi, ram, storage, task_time_submission, time_deadline_final, priority_no);
                    task.setUserId(userId);

                    this.getTaskList().add(task);

                    //going to th next task
                    task_info = task_reader.readNext();

                    taskId+=1;
                    if (task_info == null){
                        break;
                    }
                }
            }

            //saving the number od tasks per episode
            if (episodeNo == 0){
                TSPJobManager.setNumberOfTasksPerEpisode(this.getTaskList().size());
            }

            System.out.println("Dataset successfully loaded");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }
    }
}
