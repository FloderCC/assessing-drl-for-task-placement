"""
File description:

This file implements the TP_FIFO class, a placement strategy based on the First-In-First-Out (FIFO) principle.
It selects the first available server for task placement without considering any other factors.
"""

from src.strategies.strategy_base import StrategyBase
from src.munitor import monitor_tic, monitor_toc


class TP_FIFO(StrategyBase):
    """
    Placement strategy based on FIFO
    """

    def __str__(self):
        """
        Returns the string representation of the TP_FIFO strategy.
        """
        return "TP_FIFO"

    def predict(self, _, state) -> (str, float, float):
        """
        Predicts the next action based on the current state.

        Args:
            _ (int): Unused action ID.
            state (list): The current state of the environment.

        Returns:
            tuple: The chosen action, CPU percentage, and elapsed time.
        """
        # auxiliary information
        available_servers = self.get_available_servers_for_placement(state)

        if len(available_servers) == 0:
            return -1, 0, 0

        monitor_tic()
        # taking the decision
        action = available_servers[0]
        action_cpu_percent, action_elapsed_time = monitor_toc()

        return action, action_cpu_percent, action_elapsed_time