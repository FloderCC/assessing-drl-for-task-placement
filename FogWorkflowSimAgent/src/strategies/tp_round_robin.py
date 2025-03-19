"""
File description:

This file implements the TP_ROUND_ROBIN class, a placement strategy based on the Round Robin principle.
It selects servers in a cyclic order for task placement, ensuring an even distribution of tasks across servers.
"""

from src.strategies.strategy_base import StrategyBase
from src.munitor import monitor_tic, monitor_toc


class TP_ROUND_ROBIN(StrategyBase):
    """
    Placement strategy based on Round Robin
    """
    def __init__(self):
        """
        Initializes the TP_ROUND_ROBIN strategy.
        """
        super().__init__()
        self.last_selected_s_idx = None

    def __str__(self):
        """
        Returns the string representation of the TP_ROUND_ROBIN strategy.
        """
        return "TP_ROUND_ROBIN"

    def initAgent(self, devices_properties):
        """
        Initializes the agent with the given device properties.

        Args:
            devices_properties (list): Properties of the devices in the network.
        """
        super(TP_ROUND_ROBIN, self).initAgent(devices_properties)
        self.last_selected_s_idx = -1

    def go_to_next_episode(self):
        """
        Prepares the agent for the next episode by resetting the last selected server index.
        """
        self.last_selected_s_idx = -1

    def predict(self, _, state) -> (str, float, float):
        """
        Predicts the next action based on the current state.

        Args:
            _ (int): Unused action ID.
            state (list): The current state of the environment.

        Returns:
            tuple: The chosen action, CPU percentage, and elapsed time.
        """
        available_servers = self.get_available_servers_for_placement(state)

        if len(available_servers) == 0:
            return -1, 0, 0

        monitor_tic()
        selected_s_idx = next((s_idx for s_idx in available_servers if s_idx > self.last_selected_s_idx), None)
        if selected_s_idx is None:
            self.last_selected_s_idx = -1
            selected_s_idx = next((s_idx for s_idx in available_servers if s_idx > self.last_selected_s_idx), None)
        self.last_selected_s_idx = selected_s_idx

        action_cpu_percent, action_elapsed_time = monitor_toc()
        return selected_s_idx, action_cpu_percent, action_elapsed_time