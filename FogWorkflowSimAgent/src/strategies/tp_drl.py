"""
File description:

This file implements the TP_DRL class, a placement strategy based on Reinforcement Learning.
It uses a Deep Q-Learning (DQL) agent to make decisions about task placement in a network of devices.
The agent is trained to maximize rewards by choosing optimal actions based on the current state of the environment.
"""

import random
import numpy as np
from src.strategies.drl_agents.dql_tensorflow import Agent
from src.strategies.strategy_base import StrategyBase
from src.munitor import monitor_tic, monitor_toc


class TP_DRL(StrategyBase):
    """
    Placement strategy based on Reinforcement Learning
    """

    def __init__(self):
        """
        Initializes the TP_DRL strategy.
        """
        super().__init__()
        self.network_properties = None
        self.training_on = None
        self.agent = None
        self.history = None
        self.n_steep = None
        self.reward_history = {}
        self.batch_size = 64  # batch size

    def __str__(self):
        """
        Returns the string representation of the TP_DRL strategy.
        """
        return "TP_DRL"

    def initDRLAgent(self, devices_properties, network_properties, training_on):
        """
        Initializes the Deep Q-Learning agent with the given properties.

        Args:
            devices_properties (list): Properties of the devices in the network.
            network_properties (list): Properties of the network.
            training_on (bool): Flag indicating whether the agent is in training mode.
        """
        super(TP_DRL, self).initAgent(devices_properties)
        self.network_properties = network_properties
        self.training_on = training_on

        self.agent = Agent(state_dim=7 + self.num_devices * 4,
                           n_actions=self.num_devices,
                           batch_size=self.batch_size,
                           epsilon=0)  # Testing exploitation to avoid penalization due to exploration

        self.history = {}
        self.n_steep = 0

    def go_to_next_episode(self):
        """
        Prepares the agent for the next episode by resetting the environment and history.
        """
        self.history = {}
        self.n_steep = 0
        self.reward_history = {}
        self.agent.reset_environment(epsilon=0)
        print("Next episode started")

    qty_of_penalization = 0

    def predict(self, action_id, state) -> (str, float, float):
        """
        Predicts the next action based on the current state.

        Args:
            action_id (int): The ID of the current action.
            state (list): The current state of the environment.

        Returns:
            tuple: The chosen action, CPU percentage, and elapsed time.
        """
        available_servers = self.get_available_servers_for_placement(state)

        if len(available_servers) == 0:
            return -1, 0, 0

        monitor_tic()
        state = np.array(state)
        action = self.agent.choose_action(state)
        action_cpu_percent, action_elapsed_time = monitor_toc()

        if action not in available_servers:
            if self.training_on:
                self.history[action_id] = [state, action, self.punishing_reward]
                self.qty_of_penalization += 1
                action_id_module = action_id % 500
                if action_id_module == 0:
                    self.qty_of_penalization = 0

            action = random.choice(available_servers)
            return action, action_cpu_percent, action_elapsed_time

        if self.training_on:
            self.history[action_id] = [state, action, None]

        return action, action_cpu_percent, action_elapsed_time

    def save_reward(self, action_id, reward):
        """
        Saves the reward for a given action.

        Args:
            action_id (int): The ID of the action.
            reward (float): The reward to be saved.

        Returns:
            tuple: The CPU percentage and elapsed time.
        """
        if self.training_on:
            monitor_tic()
            if self.history[action_id][2] is None:
                self.history[action_id][2] = reward
                self.reward_history[action_id] = reward
            return monitor_toc()
        return 0, 0

    def learn(self, action_id, state):
        """
        Trains the agent based on the stored transitions.

        Args:
            action_id (int): The ID of the action.
            state (list): The current state of the environment.

        Returns:
            tuple: The CPU percentage and elapsed time.
        """
        if self.training_on:
            monitor_tic()
            self.n_steep += 1
            aux = self.history[action_id]
            self.agent.store_transition(aux[0], aux[1], aux[2], state)
            del self.history[action_id]
            self.agent.learn()

            if self.n_steep % 50 == 0:
                self.agent.update_target_model()

            return monitor_toc()
        return 0, 0