"""
File description:

This file manages the TSP simulation.
"""

import os
import random
import shutil
import numpy as np
import tensorflow
import torch
from torch.backends import cudnn

from strategies.strategy_base import StrategyBase
from strategies.tp_fifo import TP_FIFO
from strategies.tp_random import TP_RANDOM
from strategies.tp_round_robin import TP_ROUND_ROBIN
from strategies.tp_drl import TP_DRL
from strategies.tsp_drl import TSP_DRL

# Global variables for environment configuration
devices_properties = None  # List of device properties
has_cloud = False  # Flag indicating if cloud is present
network_properties = None  # Network properties
memory_state_action_new_state_cost = None  # Dictionary for storing state-action pairs
strategy = StrategyBase()  # Placement agent
setup_name = None  # Setup name for saving/loading models
load_pretrained_model_on = None  # Flag for loading pretrained model
model_episode_to_load = None  # Episode number to load the model from
training_on = None  # Flag for training mode
save_final_model = None  # Flag for saving the final model
random_seed = None  # Random seed for reproducibility

def setup(json_data: dict) -> str:
    """
    Set up the environment configuration.

    Args:
        json_data (dict): The environment information.

    Returns:
        str: The name of the strategy used.
    """
    global devices_properties, strategy, setup_name, load_pretrained_model_on, model_episode_to_load, training_on, save_final_model, network_properties, has_cloud, random_seed

    random_seed = json_data["random_seed"]
    set_seed(random_seed)

    devices_properties = json_data["fog"]
    cloud_properties = json_data.get("cloud")
    if cloud_properties:
        devices_properties.insert(0, cloud_properties)
        has_cloud = True

    strategy_name = json_data["strategy"]
    setup_name = json_data["setup_name"]
    print(f"Running setup name: {setup_name}")

    load_pretrained_model_on = json_data.get("load_pretrained_model_on")
    model_episode_to_load = json_data.get("model_episode_to_load")
    training_on = json_data.get("training_on")
    save_final_model = json_data.get("save_final_model_on")

    network_properties = [
        json_data.get("cloud_nodes_upload_bandwidth"),
        json_data.get("cloud_nodes_download_bandwidth"),
        json_data.get("fog_nodes_upload_bandwidth"),
        json_data.get("fog_nodes_download_bandwidth"),
        json_data.get("latency_gateway_fog_node"),
        json_data.get("latency_gateway_cloud_node")
    ]

    if strategy_name == "TP_FIFO":
        strategy = TP_FIFO()
        strategy.initAgent(devices_properties)
    elif strategy_name == "TP_RANDOM":
        strategy = TP_RANDOM()
        strategy.initAgent(devices_properties)
    elif strategy_name == "TP_ROUND_ROBIN":
        strategy = TP_ROUND_ROBIN()
        strategy.initAgent(devices_properties)
    elif strategy_name == "TP_DRL":
        strategy = TP_DRL()
        strategy.initDRLAgent(devices_properties, network_properties, training_on)
        strategy.initDRLAgent(devices_properties, json_data["priorities_quantity"], training_on)
    elif strategy_name == "TSP_DRL":
        strategy = TSP_DRL()
        strategy.initDRLAgent(devices_properties, json_data["priorities_quantity"], training_on)

    if load_pretrained_model_on and "DRL" in strategy.__str__():
        input_directory = f'models/{setup_name}'
        strategy.agent.load(f'{input_directory}/episode_{model_episode_to_load}/model')

    return strategy_name

def ask_decision(action_id, state: list) -> (str, float, float):
    """
    Ask for a decision from the placement agent.

    Args:
        action_id (int | None): The ID of the cloudlet to be placed or None if the strategy has scheduling.
        state (list): The current cloudlet's TSP task information and the server status.

    Returns:
        tuple: The selected node to allocate the task, the used CPU percentage, and the used time.
    """
    global strategy
    return strategy.predict(action_id, state)

def save_reward(action_id: int, reward: float) -> (float, float):
    """
    Save the reward for a given cloudlet ID to be used with the next state.

    Args:
        action_id (int | None): The ID of the cloudlet to be placed or None if the strategy has scheduling.
        reward (float): The reward of the placement done.

    Returns:
        tuple: The used CPU percentage and the used time.
    """
    return strategy.save_reward(action_id, reward)

def retrain(action_id: int, state: list) -> (float, float):
    """
    Retrain the RL agent.

    Args:
        action_id (int | None): The ID of the cloudlet to be placed or None if the strategy has scheduling.
        state (list): The next cloudlet's TSP task information and the next server status.

    Returns:
        tuple: The used CPU percentage and the used time.
    """
    return strategy.learn(action_id, np.array(state))

def plot(plot_name: str, values: list) -> str:
    """
    Plot a value series and its average.

    Args:
        plot_name (str): The name of the plot.
        values (list): The value series.

    Returns:
        str: The process result.
    """
    import matplotlib.pyplot as plt

    fig, ax = plt.subplots(1)
    x = list(range(len(values)))
    y = values
    ax.plot(x, y, label=plot_name)

    avg_values = [sum(values[:i+1]) / (i+1) for i in range(len(values))]
    ax.plot(x, avg_values, label='Average ' + plot_name, linestyle='--')

    plt.xlabel("Task")
    plt.ylabel(plot_name)
    plt.legend()
    plt.show()
    return "Success"

def save_model(episode_number: int) -> str:
    """
    Save the model as a file.

    Args:
        episode_number (int): The episode number.

    Returns:
        str: The process result.
    """
    if save_final_model and "DRL" in strategy.__str__():
        output_directory = f'models/{setup_name}/episode_{episode_number}'
        if os.path.exists(output_directory):
            shutil.rmtree(output_directory)
        os.makedirs(output_directory)
        strategy.agent.save(f'{output_directory}/model')
    return "Success"

def next_episode() -> str:
    """
    Go to the next episode.

    Returns:
        str: The process result.
    """
    set_seed(random_seed)
    strategy.go_to_next_episode()
    return "Success"

def set_seed(seed):
    """
    Set a seed for all the libraries used.

    Args:
        seed (int): The seed value.
    """
    os.environ['PYTHONHASHSEED'] = str(seed)
    random.seed(seed)
    tensorflow.random.set_seed(seed)
    tensorflow.experimental.numpy.random.seed(seed)
    np.random.seed(seed)
    os.environ['TF_DETERMINISTIC_OPS'] = '1'
    os.environ['TF_CUDNN_DETERMINISTIC'] = '1'
    tensorflow.config.threading.set_inter_op_parallelism_threads(1)
    tensorflow.config.threading.set_intra_op_parallelism_threads(1)

    torch.manual_seed(seed)
    torch.cuda.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    cudnn.deterministic = True
    cudnn.benchmark = False