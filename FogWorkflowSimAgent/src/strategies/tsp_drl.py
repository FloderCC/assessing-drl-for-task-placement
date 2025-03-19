"""
File description:

This file implements the TSP_DRL class, a scheduling (regarding task priorities) and placement strategy based on Reinforcement Learning.
It uses a Deep Q-Learning (DQL) agent to make decisions about task placement in a network of devices.
The agent is trained to maximize rewards by choosing optimal actions based on the current state of the environment.
"""

import random
import numpy as np
from src.strategies.drl_agents.dql_tensorflow import Agent
from src.strategies.strategy_base import StrategyBase
from src.munitor import monitor_tic, monitor_toc

class TSP_DRL(StrategyBase):
    """
    Placement strategy based on Reinforcement Learning
    """

    def __init__(self):
        """
        Initializes the TSP_DRL strategy.
        """
        super().__init__()
        self.training_on = None
        self.agent = None
        self.history = None
        self.n_steep = None
        self.reward_history = {}
        self.batch_size = 64  # batch size
        self.priorities_quantity = None

    def __str__(self):
        """
        Returns the string representation of the TSP_DRL strategy.
        """
        return "TSP_DRL"

    def initDRLAgent(self, devices_properties, priorities_quantity: int, training_on):
        """
        Initializes the Deep Q-Learning agent with the given properties.

        Args:
            devices_properties (list): Properties of the devices in the network.
            priorities_quantity (int): The number of task priorities.
            training_on (bool): Flag indicating whether the agent is in training mode.
        """
        super(TSP_DRL, self).initAgent(devices_properties)
        self.training_on = training_on
        self.priorities_quantity = priorities_quantity

        self.agent = Agent(state_dim=6 * priorities_quantity + 1 + self.num_devices * 4,
                           n_actions=priorities_quantity * self.num_devices,
                           batch_size=self.batch_size,
                           learning_rate=0.001,
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

    def predict(self, action_id, state) -> (str, float, float):
        """
        Predicts the next action based on the current state.

        Args:
            action_id (int): The ID of the current action.
            state (list): The current state of the environment.

        Returns:
            tuple: The chosen action, CPU percentage, and elapsed time.
        """
        rl_state, first_task_by_priority = self.parse_state_to_rl(state)
        available_codified_options = self.get_available_codified_options(rl_state)

        if not available_codified_options:
            return -1, 0, 0

        monitor_tic()
        rl_state = np.array(rl_state)
        action = self.agent.choose_action(rl_state)
        action_cpu_percent, action_elapsed_time = monitor_toc()

        if action not in available_codified_options:
            if self.training_on:
                self.history[action_id] = [rl_state, action, self.punishing_reward]
            action = random.choice(available_codified_options)
            return str(first_task_by_priority[action // self.num_devices]) + "," + str(action % self.num_devices), action_cpu_percent, action_elapsed_time

        if self.training_on:
            self.history[action_id] = [rl_state, action, None]

        return str(first_task_by_priority[action // self.num_devices]) + "," + str(action % self.num_devices), action_cpu_percent, action_elapsed_time

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
            rl_state, _ = self.parse_state_to_rl(state.tolist())
            monitor_tic()
            self.n_steep += 1
            aux = self.history[action_id]
            self.agent.store_transition(aux[0], aux[1], aux[2], rl_state)
            del self.history[action_id]
            self.agent.learn()

            if self.n_steep % 100 == 0:
                self.agent.update_target_model()

            return monitor_toc()
        return 0, 0

    def parse_state_to_rl(self, state):
        """
        Parses the state to create the RL state representation.

        Args:
            state (list): The current state of the environment.

        Returns:
            tuple: The RL state representation and the first task by priority.
        """
        rl_state = []
        first_task_by_priority = self.get_first_task_by_priority(state)

        for p in range(self.priorities_quantity):
            if p + 1 in first_task_by_priority:
                t_idx = first_task_by_priority[p + 1]
                task = state[t_idx * 6: t_idx * 6 + 6]
            else:
                task = [self.empty_value] * 6

            rl_state += task

        return rl_state + state[-(self.num_devices * 4 + 1):], first_task_by_priority

    def get_available_codified_options(self, rl_state):
        """
        Gets the available options considering the tasks priorities and the servers status.

        Args:
            rl_state (list): The RL state representation.

        Returns:
            list: The available codified options.
        """
        options = []
        for t_idx in range(self.priorities_quantity):
            t_priority = rl_state[t_idx * 6 + 5]

            if t_priority == self.empty_value:
                continue

            for s_idx in range(self.num_devices):
                if rl_state[self.priorities_quantity * 6 + s_idx * 4 + 4] == 0 and \
                        (self.devices_properties[s_idx][1] >= rl_state[t_idx * 6 + 1]) and \
                        (self.devices_properties[s_idx][2] >= rl_state[t_idx * 6 + 2]):
                    options.append(t_priority * self.num_devices + s_idx)
        return options