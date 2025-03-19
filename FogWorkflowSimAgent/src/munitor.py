"""
File description:

Auxiliary file for measuring the execution time and CPU utilization.
"""

import threading
import time
import psutil

# Global variables for resource usage measurement
thread_running = None  # Flag to control the measurement thread
cpu_thread = None  # Thread object for CPU measurement
start_time = None  # Start time of the measurement
avg_cpu_percentage = None  # Average CPU percentage during the measurement

# Measure interval in seconds (minimum is 0.01)
measure_interval = 0.01

def measure_cpu():
    """
    Continuously measure the CPU percentage and calculate the average.

    This function runs in a separate thread and updates the global
    variable `avg_cpu_percentage` with the average CPU usage.
    """
    global avg_cpu_percentage, measure_interval

    cpu_load_sum = 0  # Sum of CPU load percentages
    cpu_load_quantity = 0  # Number of CPU load measurements

    while thread_running:
        cpu_percentage = psutil.cpu_percent(interval=measure_interval, percpu=False)
        cpu_load_sum += cpu_percentage
        cpu_load_quantity += 1
        time.sleep(measure_interval)

    avg_cpu_percentage = cpu_load_sum / cpu_load_quantity

def monitor_tic():
    """
    Start the CPU usage measurement and record the start time.

    This function initializes the global variables and starts the
    measurement thread.
    """
    global thread_running, cpu_thread, start_time

    thread_running = True
    cpu_thread = threading.Thread(target=measure_cpu)
    cpu_thread.start()
    start_time = time.perf_counter()

def monitor_toc():
    """
    Stop the CPU usage measurement and calculate the elapsed time.

    This function stops the measurement thread, calculates the average
    CPU usage, and returns the average CPU usage and elapsed time.

    Returns:
        tuple: A tuple containing the average CPU usage (float) and the
               elapsed time (float) in seconds.
    """
    global thread_running, cpu_thread, start_time, avg_cpu_percentage

    # Stop measuring time
    end_time = time.perf_counter()

    # Stop the CPU measurement thread
    thread_running = False
    cpu_thread.join()

    # In case this function is called in less than measure_interval seconds since the tic function
    while avg_cpu_percentage == 0:
        time.sleep(measure_interval)
        avg_cpu_percentage = psutil.cpu_percent(interval=measure_interval, percpu=False)

    # Calculate the elapsed time
    return avg_cpu_percentage, end_time - start_time