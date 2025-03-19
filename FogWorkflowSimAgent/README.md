## Task Scheduling and Placement strategies
This repository's project has the following structure:
```
├── src/
│   ├── models/                         # Directory with the trained models (id they are saved)
│   ├── plots/                          # Directory with the experiment results plots
│   ├── simulation_results/             # Directory with the WorkFlowSim simulation results
│   ├── strategies/                     # Directory with the strategies
│       ├── drl_agents/                 # Directory with the DRL agents
│           └── dql_tensorflow.py       # DRL agent based on Deep Q-Learning
│       ├── strategy_base.py            # Base class for the strategies
│       ├── tp_fifo.py                  # FIFO strategy for task placement
│       ├── tp_random.py                # Random strategy for task placement
│       ├── tp_round_robin.py           # Round-robin strategy for task placement
│       ├── tp_drl.py                   # DRL-based strategy for task placement
│       └── tsp_drl.py                  # DRL-based strategy for task scheduling and placement
│   ├── munitor.py                      # Auxiliary script for measuring the execution time and CPU utilization
│   ├── plot_simulation_results.py      # Plots the simulation results
│   ├── tsp_manager.py                  # Manages the TSP simulation
│   └── tsp_socket_server.py            # Socket server for the TSP simulation
├── README.md                           # Main file with the project description
└── requirements.txt                    # Required Python packages
```

