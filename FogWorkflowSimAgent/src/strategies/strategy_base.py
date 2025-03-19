"""
File description:

This file implements the StrategyBase class, which serves as a base class for different task placement strategies.
It provides common functionalities and interfaces that derived classes can use and override.
"""

class StrategyBase(object):
    """
    Base class for task placement strategies.
    """

    def __init__(self):
        """
        Initializes the StrategyBase class.
        """
        self.devices_properties = None
        self.num_devices = None
        self.punishing_reward = -1
        self.empty_value = 0

    def __str__(self):
        """
        Returns the string representation of the strategy.
        """
        pass

    def initAgent(self, devices_properties):
        """
        Initializes the agent with the given device properties.

        Args:
            devices_properties (list): Properties of the devices in the network.
        """
        self.devices_properties = devices_properties
        self.num_devices = len(devices_properties)

    def predict(self, action_id, state) -> (str, float, float):
        """
        Predicts the next action based on the current state.

        Args:
            action_id (int): The ID of the current action.
            state (list): The current state of the environment.

        Returns:
            tuple: The chosen action, CPU percentage, and elapsed time.
        """
        pass

    def save_reward(self, action_id, reward) -> (float, float):
        """
        Saves the reward for a given action.

        Args:
            action_id (int): The ID of the action.
            reward (float): The reward to be saved.

        Returns:
            tuple: The CPU percentage and elapsed time.
        """
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
        return 0, 0

    def get_available_servers_for_placement(self, state):
        """
        Gets the available servers with enough capacity for placing the task.

        Args:
            state (list): The current state of the environment.

        Returns:
            list: The indices of available servers.
        """
        nodes_state = state[7:]
        qty_of_nodes = int(len(nodes_state) / 4)

        return [i for i in range(qty_of_nodes)
                if int(nodes_state[i * 4 + 3]) == 0 and self.devices_properties[i][1] >= state[1] and self.devices_properties[i][2] >= state[2]
                ]

    def get_first_task_by_priority(self, state):
        """
        Gets the first task by priority that can be placed on an available server.

        Args:
            state (list): The current state of the environment.

        Returns:
            dict: A dictionary mapping task priorities to task indices.
        """
        state_task_size = len(state) - (self.num_devices * 4 + 1)

        first_task_by_priority = {}

        for t_idx in range(int(state_task_size / 6)):
            task_priority = state[t_idx * 6 + 5]
            if task_priority not in first_task_by_priority:

                for s_idx in range(self.num_devices):
                    if state[state_task_size + 1 + s_idx * 4 + 3] == 0 and (
                            self.devices_properties[s_idx][1] >= state[t_idx * 6 + 1]) and (
                            self.devices_properties[s_idx][2] >= state[t_idx * 6 + 2]):
                        first_task_by_priority[task_priority] = t_idx
                        break

        return first_task_by_priority

    def go_to_next_episode(self):
        """
        Prepares the agent for the next episode.
        """
        pass