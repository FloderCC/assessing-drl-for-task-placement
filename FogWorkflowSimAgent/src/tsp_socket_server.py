"""
File description:

This files serves the TSP agents through a socket server.
"""

import json
import socket
import traceback
from tsp_manager import *

def server_program() -> str:
    """
    Python websocket server for receiving the calls regarding the task placement.

    This function sets up a server that listens for incoming connections and processes
    various actions such as setup, ask_decision, save_reward, retrain, plot, save_model,
    and next_episode. It communicates with the client using JSON messages.

    Returns:
        str: The name of the strategy used.
    """
    host = "192.168.94.145"
    port = 5000  # initiate port no above 1024

    # creating the connection endpoint
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # bind host address and port together
    server_socket.bind((host, port))

    # configure how many clients the server can listen to simultaneously
    server_socket.listen(1)

    print("Waiting for connections")
    # accept new connection
    conn, address = server_socket.accept()

    # for storing the strategy name
    strategy_name = None

    # auxiliary variables for metering the power consumption
    cpu_percent_list = []
    time_list = []

    while True:
        try:
            data = b''
            while True:
                part = conn.recv(1024)
                if not part:
                    break
                data += part
                if b'\n' in part:  # Check for the newline delimiter
                    break

            # Remove the delimiter and decode
            data = data.strip().decode('utf-8')

            if not data or data == "":
                break

            # parse the received information to JSON
            received_info = json.loads(data)

            action = received_info["action"]
            response = None

            if action == "setup":
                # setup algorithms and seed
                strategy_name = response = setup(json_data=received_info["data"])

                cpu_percent_list = []
                time_list = []

            elif action == "ask_decision":
                response, action_cpu_percent, action_elapsed_time = ask_decision(
                    action_id=received_info["data"]["action_id"],
                    state=received_info["data"]["state"]
                )

                # registering the CPU percentage and the elapsed time
                cpu_percent_list.append(action_cpu_percent)
                time_list.append(action_elapsed_time)

                # computing the total time
                action_id_time = sum(time_list)

                # computing the average CPU weighted by the time
                action_id_percentage = sum(
                    [cpu_percent_list[i] * time_list[i] for i in range(len(cpu_percent_list))]
                ) / (action_id_time if action_id_time > 0 else 1)

                cpu_percent_list = []
                time_list = []

                response = f"{action_id_time},{action_id_percentage}d{response}"

            elif action == "save_reward":
                action_cpu_percent, action_elapsed_time = save_reward(
                    action_id=received_info["data"]["action_id"],
                    reward=received_info["data"]["reward"]
                )
                response = "Success"
                cpu_percent_list.append(action_cpu_percent)
                time_list.append(action_elapsed_time)

            elif action == "retrain":
                action_cpu_percent, action_elapsed_time = retrain(
                    action_id=received_info["data"]["action_id"],
                    state=received_info["data"]["state"]
                )
                response = "Success"
                cpu_percent_list.append(action_cpu_percent)
                time_list.append(action_elapsed_time)

            elif action == "plot":
                response = plot(received_info["plot_name"], values=received_info["data"])

            elif action == "save_model":
                response = save_model(received_info["episode_number"])

            elif action == "next_episode":
                response = next_episode()

            message_to_send = str(response).encode("UTF-8")
            conn.send(len(message_to_send).to_bytes(2, 'big'))
            conn.send(message_to_send)

        except Exception:
            print(traceback.format_exc())
            break

    conn.close()  # close the connection

    return strategy_name


if __name__ == '__main__':
    # starting the server
    server_program()