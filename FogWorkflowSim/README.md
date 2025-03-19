## Task Scheduling and Placement Simulator

This repository's project is a fork of the [**FogWorkFlowSim**](https://github.com/ISEC-AHU/FogWorkflowSim) project, which was extended to conduct experiments on Task Scheduling and Placement (TSP). To achieve this goal, a set of classes was added without modifying the original code. These added classes start with the name 'TSP'. They are:

- `org.fog.test.perfeval.TSPApp`: Simulates a task scheduling and placement application.
- `org.workflowsim.scheduling.TSPBaseStrategyAlgorithm`: Base class for task scheduling and placement algorithms.
- `org.fog.entities.TSPController`: Extends the `Controller` class for the TSP problem. It includes the evaluation indicators to calculate the performance metrics.
- `org.workflowsim.utils.TSPDecisionResult`: Represents the result of a decision made by a TSP strategy.
- `org.workflowsim.utils.TSPEnvHelper`: A helper class providing useful methods for TSP simulation.
- `org.fog.entities.TSPFogBroker`: Extends the `FogBroker` class, adding TSP-specific schedulers.
- `org.workflowsim.TSPJob`: Defines execution constraints for related tasks.
- `org.workflowsim.utils.TSPJobManager`: Manages task execution constraints specified in `TSPJob` objects.
- `org.workflowsim.scheduling.TSPPlacementAlgorithm`: Implements the task placement algorithm.
- `org.workflowsim.scheduling.TSPSchedulingAndPlacementAlgorithm`: Implements both task scheduling and placement algorithms.
- `org.workflowsim.utils.TSPSocketClient`: A client socket for TSP simulation, containing methods for transferring information with the selected strategy code in another service.
- `org.workflowsim.utils.TSPSocketRequest`: Manages sending requests to a server and receiving responses.
- `org.workflowsim.TSPTask`: Extends the `Task` class and defines the characteristics of a task in the TSP simulation.
- `org.workflowsim.TSPWorkflowParser`: Replaces the `WorkflowParser` class for TSP problems, handling dataset loading and task/dependency creation.
- `org.workflowsim.TSPWorkflowPlanner`: Replaces the `WorkflowPlanner` class for TSP problems, managing task parsing and scheduling within the simulation.