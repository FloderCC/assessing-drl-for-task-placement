# Assessing DRL for task placement

### Repository of the work entitled "Intelligent Task Placement: Assessing the Cost of Deep Reinforcement Learning"

## Structure

This repository contains the following projects:

- `FogWorkflowSim/`: Contains the simulator, which is a fork of the original [FogWorkflowSim](https://github.com/ISEC-AHU/FogWorkflowSim) project. The simulator was extended to include DRL-based task placement strategies.
- `FogWorkflowSimAgent/`: - `FogWorkflowSimAgent/`: Contains the task placement strategies and serves them via a WebSocket for evaluation in the simulator.

## How to Use

To run the simulator, the `FogWorkflowSimAgent` project should be deployed on an isolated computing device. The file to execute is `tsp_socket_server.py`. This file will start a server that the simulator will use to communicate with the strategies.

Next, the `FogWorkflowSim` project should be deployed in another environment. The file to execute is `org.fog.test.perfeval.TSPApp.java`. This file will start the simulation and communicate with the server to obtain the strategies' decisions.
