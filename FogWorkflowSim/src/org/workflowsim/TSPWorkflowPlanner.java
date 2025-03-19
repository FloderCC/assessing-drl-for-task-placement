package org.workflowsim;
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.workflowsim.utils.Parameters;

/**
 * Since the WorkflowPlanner is defined as "final" this class replaces the WorkflowPlanner class for TSP problems.
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */
public final class TSPWorkflowPlanner extends SimEntity {

    /**
     * The workflow parser.
     */
    protected TSPWorkflowParser parser;
    /**
     * The associated clustering engine.
     */

    /**
     * Created a new TSPWorkflowPlanner object.
     *
     * @param name name to be associated with this entity (as required by
     * Sim_entity class from simjava package)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public TSPWorkflowPlanner(String name) throws Exception {
        this(name, 1);
    }

    public TSPWorkflowPlanner(String name, int schedulers) throws Exception {
        super(name);

//        setTaskList(new ArrayList<>());
//        this.clusteringEngine = new TSPClusteringEngine(name + "_Merger_", schedulers);
//        this.clusteringEngineId = this.clusteringEngine.getId();
//        this.parser = new TSPWorkflowParser(getClusteringEngine().getWorkflowEngine().getSchedulerId(0));

        this.workflowEngineId = new WorkflowEngine(name + "_Engine", schedulers).getId();

//        this.parser = new TSPWorkflowParser(this.workflowEngineId);
        this.parser = new TSPWorkflowParser(getWorkflowEngine().getSchedulerId(0));

    }

    /**
     * Gets the workflow parser
     *
     * @return the workflow parser
     */
    public TSPWorkflowParser getWorkflowParser() {
        return this.parser;
    }

    private int workflowEngineId;

    /**
     * Gets the workflow engine
     *
     * @return the workflow engine
     */
    public WorkflowEngine getWorkflowEngine() {
        return (WorkflowEngine) CloudSim.getEntity(this.workflowEngineId);
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case WorkflowSimTags.START_SIMULATION:
                getWorkflowParser().parse();

                List<Task> taskList = getWorkflowParser().getTaskList();

                int brokerId = CloudSim.getEntityId("MyFogScheduler");
                // scheduling the Job (tasks) arrivals
                for (Task task : taskList) {
                    schedule(brokerId, ((TSPTask)task).getArrivalTime(), WorkflowSimTags.JOB_SUBMIT, task);
                }
                scheduleNow(brokerId, CloudSimTags.SEND_QTY_OF_TASKS, taskList.size());
                getWorkflowEngine().setQuantityOfTasks(taskList.size());
                break;

            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Overrides this method when making a new and different type of Broker.
     * This method is called by {@link #body()} for incoming unknown tags.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
            return;
        }

        Log.printLine(getName() + ".processOtherEvent(): "
                + "Error - event unknown by this DatacenterBroker.");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#startEntity()
     */
    @Override
    public void startEntity() {
        Log.printLine("Starting WorkflowSim " + Parameters.getVersion());
        Log.printLine(getName() + " is starting...");
        schedule(getId(), 0, WorkflowSimTags.START_SIMULATION);
    }

}
