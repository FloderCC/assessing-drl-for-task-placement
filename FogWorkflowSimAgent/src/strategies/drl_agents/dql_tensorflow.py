"""
File description:

File containing the implementation of a Deep Q-Learning Agent using TensorFlow and Keras.
"""

import random
import keras
import numpy as np


class ReplayBuffer:
    """
    A class used to represent a Replay Buffer for storing and sampling experiences.

    Attributes:
        buffer_size (int): The maximum size of the buffer.
        buffer (list): The list to store experiences.
    """

    def __init__(self, buffer_size):
        """
        Initializes the ReplayBuffer with a specified size.

        Args:
            buffer_size (int): The maximum size of the buffer.
        """
        self.buffer_size = buffer_size
        self.buffer = []

    def add(self, state, action, reward, next_state):
        """
        Adds a new experience to the buffer.

        Args:
            state (array): The current state.
            action (int): The action taken.
            reward (float): The reward received.
            next_state (array): The next state.
        """
        self.buffer.append((state, action, reward, next_state))
        if self.length() > self.buffer_size:
            self.buffer.pop(0)

    def sample(self, batch_size):
        """
        Samples a batch of experiences from the buffer.

        Args:
            batch_size (int): The number of experiences to sample.

        Returns:
            array: A batch of sampled experiences.
        """
        return np.array(self.buffer, dtype=object)[np.random.choice(self.length(), batch_size, replace=False)]

    def all(self):
        """
        Returns all experiences in the buffer.

        Returns:
            list: All experiences in the buffer.
        """
        return self.buffer

    def clear(self):
        """
        Clears all experiences from the buffer.
        """
        self.buffer.clear()

    def length(self):
        """
        Returns the current number of experiences in the buffer.

        Returns:
            int: The number of experiences in the buffer.
        """
        return len(self.buffer)


def create_q_network(state_size, action_size, learning_rate):
    """
    Creates a Q-network model.

    Args:
        state_size (int): The size of the state space.
        action_size (int): The size of the action space.
        learning_rate (float): The learning rate for the optimizer.

    Returns:
        keras.Model: The Q-network model.
    """
    model = keras.models.Sequential()
    model.add(keras.layers.InputLayer(shape=(state_size,)))
    model.add(keras.layers.Dense(256, activation='relu'))
    model.add(keras.layers.Dense(256, activation='relu'))
    model.add(keras.layers.Dense(action_size))
    model.compile(optimizer=keras.optimizers.Adam(learning_rate=learning_rate), loss='mse')
    return model


class Agent:
    """
    A class used to represent a DQN Agent.

    Attributes:
        epsilon (float): The exploration rate.
        epsilon_min (float): The minimum exploration rate.
        epsilon_decay (float): The decay rate of epsilon.
        state_size (int): The size of the state space.
        action_size (int): The size of the action space.
        gamma (float): The discount factor.
        batch_size (int): The size of the training batch.
        model (keras.Model): The Q-network model.
        target_model (keras.Model): The target Q-network model.
        replay_buffer (ReplayBuffer): The replay buffer for storing experiences.
    """

    def __init__(self, state_dim, batch_size, n_actions,
                 gamma=0.99, learning_rate=0.001, buffer_size=256,
                 epsilon=1.0, epsilon_min=0.01, epsilon_decay=0.999):
        """
        Initializes the Agent with the specified parameters.

        Args:
            state_dim (int): The size of the state space.
            batch_size (int): The size of the training batch.
            n_actions (int): The size of the action space.
            gamma (float): The discount factor.
            learning_rate (float): The learning rate for the optimizer.
            buffer_size (int): The maximum size of the replay buffer.
            epsilon (float): The initial exploration rate.
            epsilon_min (float): The minimum exploration rate.
            epsilon_decay (float): The decay rate of epsilon.
        """
        self.epsilon = epsilon
        self.epsilon_min = epsilon_min
        self.epsilon_decay = epsilon_decay
        self.state_size = state_dim
        self.action_size = n_actions
        self.gamma = gamma
        self.batch_size = batch_size

        self.model = create_q_network(self.state_size, self.action_size, learning_rate)
        print("Model created")

        self.target_model = create_q_network(state_dim, n_actions, learning_rate)
        self.update_target_model()

        self.replay_buffer = ReplayBuffer(buffer_size)

    def reset_environment(self, epsilon=1.0):
        """
        Resets the environment and replay buffer.

        Args:
            epsilon (float): The exploration rate to reset to.
        """
        self.replay_buffer.clear()
        self.epsilon = epsilon

    def update_target_model(self):
        """
        Updates the target model with the weights of the main model.
        """
        self.target_model.set_weights(self.model.get_weights())
        print("Target model updated")

    def store_transition(self, state, action, reward, new_state):
        """
        Stores a transition in the replay buffer.

        Args:
            state (array): The current state.
            action (int): The action taken.
            reward (float): The reward received.
            new_state (array): The next state.
        """
        self.replay_buffer.add(state, action, reward, new_state)

    def choose_action(self, state):
        """
        Chooses an action based on the current state using an epsilon-greedy policy.

        Args:
            state (array): The current state.

        Returns:
            int: The action chosen.
        """
        if np.random.rand() <= self.epsilon:
            return random.randrange(self.action_size)
        q_values = self.model.predict(np.expand_dims(state, axis=0), verbose=0)
        return np.argmax(q_values[0])

    def choose_valid_action(self, state, available_servers):
        """
        Chooses a valid action from the available servers based on the current state using an epsilon-greedy policy.

        Args:
            state (array): The current state.
            available_servers (list): The list of available servers.

        Returns:
            int: The action chosen.
        """
        if np.random.rand() < self.epsilon:
            return np.random.choice(available_servers)
        else:
            q_values = self.model.predict(np.array([state]), verbose=0)
            return available_servers[np.argmax(q_values[0][available_servers])]

    def learn(self):
        """
        Trains the model using experiences sampled from the replay buffer.
        """
        if self.replay_buffer.length() < self.batch_size:
            return

        batch = self.replay_buffer.sample(self.batch_size)
        states, actions, rewards, next_states = zip(*batch)

        states = np.array(states)
        actions = np.array(actions)
        rewards = np.array(rewards)
        next_states = np.array(next_states)

        q_values = self.model.predict(states, verbose=0)
        q_values_next = self.target_model.predict(next_states, verbose=0)

        for i in range(self.batch_size):
            q_values[i][actions[i]] = rewards[i] + self.gamma * np.amax(q_values_next[i])

        self.model.fit(states, q_values, epochs=1, verbose=0, batch_size=self.batch_size)

        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

    def save(self, file_path):
        """
        Saves the model weights to a file.

        Args:
            file_path (str): The path to save the model weights.
        """
        print("Saving")
        self.model.save_weights(file_path + ".weights.h5")

    def load(self, file_path):
        """
        Loads the model weights from a file.

        Args:
            file_path (str): The path to load the model weights from.
        """
        print("Loading")
        self.model.load_weights(file_path + ".weights.h5")