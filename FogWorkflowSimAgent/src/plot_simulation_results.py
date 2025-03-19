"""
File description:

This file plots the simulation results.
"""

import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from pandas import DataFrame

def load_results() -> DataFrame:
    """
    Load the results from CSV files and concatenate them into a single DataFrame.

    Returns:
        DataFrame: A DataFrame containing the concatenated results.
    """
    results_directory = "simulation_results/"

    # Load the results of the models without costs
    results_without_cost = pd.read_csv(results_directory + 'results_without_costs.csv')

    # Add 'L_' before the strategy name to indicate it's a Light version of the strategy
    results_without_cost['Strategy'] = 'L_' + results_without_cost['Strategy']

    # Load the results of the models with costs
    results_with_cost = pd.read_csv(results_directory + 'results_with_costs.csv')

    # Concatenate the two DataFrames
    full_df = pd.concat([results_with_cost, results_without_cost], ignore_index=True)
    return full_df

# Load and process the results
results: DataFrame = load_results()

# Average the results of the seeds
results = results.groupby(['Strategy', 'Dataset', 'Episode']).mean().reset_index()

# Rename the strategy names for better readability
results['Strategy'] = results['Strategy'].replace({
    'TP_FIFO': 'FIFO',
    'TP_RANDOM': 'Random',
    'TP_ROUND_ROBIN': 'RR',
    'TP_DRL': 'TP',
    'TSP_DRL': 'TSP',
    'L_TP_FIFO': 'L_FIFO',
    'L_TP_RANDOM': 'L_Random',
    'L_TP_ROUND_ROBIN': 'L_RR',
    'L_TP_DRL': 'L_TP',
    'L_TSP_DRL': 'L_TSP',
})

# Define custom color palette for the strategies
custom_palette = {
    "TP": "#e85d5e",  # Red
    "TSP": "#377eb8",  # Blue
    "FIFO": "#ff7f00",  # Orange
    "Random": "#984ea3",  # Purple
    "RR": "#dede00",  # Yellow
}

# Define custom color palette for the light versions of the strategies
custom_palette_lv = {}

# Repeat the colors for the light versions of the strategies
for strategy in list(custom_palette.keys()):
    custom_palette_lv['L_' + strategy] = custom_palette[strategy]

# Combine the two palettes
custom_palette = {**custom_palette, **custom_palette_lv}
legend_order = list({**custom_palette_lv, **custom_palette}.keys())

# Define custom line styles for the strategies
custom_line_styles = {strategy: 'dotted' if 'L_' in strategy else '-' for strategy in custom_palette.keys()}

# Convert P1 from value to percentage (total was 720)
results['P1'] = results['P1'] / 720 * 100

# Define a mapping for renaming the target columns
target_rename_map = {
    'Avg task time': 'Average Task Response Time (s)',
    'Avg Reward': 'Average Reward',
    'Gateway busy energy': 'Gateway Energy Consumption in Busy State (J)',
    'Gateway total energy': 'Gateway Energy Consumption (J)',
    'Total energy': 'System Energy Consumption (J)',
    'P1': 'Missed Deadlines for Priority 1 Tasks (%)'
}

# Define the possible targets for plotting
targets = ['Avg task time', 'Avg Reward', 'Gateway busy energy', 'Gateway total energy', 'P1', 'Total energy']

# Separate the strategies into light and weighted groups
all_strategies = list(custom_palette.keys())
light_strategies = [strategy for strategy in all_strategies if 'L_' in strategy]
weighted_strategies = [strategy for strategy in all_strategies if strategy not in light_strategies]

strategy_groups = {
    'L ': light_strategies,
    'W ': weighted_strategies
}

# Determine the maximum and minimum values for each target
y_max_and_min_mby_target = {}
for target in targets:
    target_max = results[target].max()
    target_min = results[target].min()
    y_max_and_min_mby_target[target] = (target_max, target_min)

# Plot the results for each strategy group and target
for strategy_group in strategy_groups.keys():
    strategies_tp_plot = strategy_groups[strategy_group]
    font_size = 18
    for target in targets:
        # Get the target max and min values
        target_max, target_min = y_max_and_min_mby_target[target]

        # Rename the target column
        results_to_plot = results.rename(columns={target: target_rename_map[target]}).copy()
        target = target_rename_map[target]

        # Skip certain targets for the light strategies
        if strategy_group == 'L ':
            if 'Gateway Energy Consumption in Busy State' in target:
                continue
            if 'System Energy Consumption (J)' == target:
                continue

        # Create the plot
        fig, ax = plt.subplots(figsize=(7, 5))
        plt.rcParams.update({'font.size': font_size})  # Set the font size globally

        # Loop through each strategy and plot it
        for strategy in strategies_tp_plot:
            strategy_data = results_to_plot[results_to_plot['Strategy'] == strategy].copy()
            strategy_data[target] = strategy_data[target].rolling(window=5).mean()
            sns.lineplot(
                data=strategy_data, x='Episode', y=target, label=strategy,
                color=custom_palette[strategy], linestyle=custom_line_styles[strategy], ax=ax, linewidth=2
            )

        # Customize the plot
        ax.set_xlabel("Episodes", fontsize=font_size)
        if target == 'Missed Deadlines for Priority 1 Tasks (%)':
            ax.set_ylabel(target, fontsize=font_size-3)
        elif 'Gateway Energy Consumption in Busy State' in target:
            ax.set_ylabel(target, fontsize=font_size-4.5)
        else:
            ax.set_ylabel(target, fontsize=font_size)
        ax.tick_params(axis='both', labelsize=font_size)
        ax.legend(title='Strategy', bbox_to_anchor=(1.00, 1.075), loc='upper left')  # Place legend outside the plot

        # Update the legend order
        legend_order_filtered = [legend for legend in legend_order if legend in strategies_tp_plot]
        handles, labels = ax.get_legend_handles_labels()
        ax.legend([handles[labels.index(legend)] for legend in legend_order_filtered], legend_order_filtered, title='Strategy', bbox_to_anchor=(1.00, 1.075), loc='upper left')

        # Save the plot as PDF and PNG
        target_safe = target.replace(' (%)', '')
        plt.savefig(f'./plots/pdf/{strategy_group}{target_safe}.pdf', bbox_inches='tight')
        plt.savefig(f'./plots/png/{strategy_group}{target_safe}.png', bbox_inches='tight')

        # Export the plotted data to a CSV file
        data_to_export = results_to_plot[['Strategy', 'Episode', target]]

        # Filter to include only the strategies in strategies_tp_plot
        data_to_export = data_to_export[data_to_export['Strategy'].isin(strategies_tp_plot)]

        # Round the target values to 4 decimal places
        data_to_export[target] = data_to_export[target].round(4)

        # Uncomment the following line to save the data to a CSV file
        # data_to_export.to_csv(f'./plots/{strategy_group}{target}.csv', index=False)