package org.workflowsim.utils;

/**
 * TSPDecisionResult is a class that represents the result of a decision made by the TSP algorithm.
 * It contains the time it took to make the decision and the action that was taken.
 *
 * @since TSP Extension 1.0
 * @author Julio Corona
 */

public class TSPDecisionResult {
    private final double time;
    private final int[] action;

    public TSPDecisionResult(double time, int[] action) {
        this.time = time;
        this.action = action;
    }

    public double getTime() {
        return time;
    }

    public int[] getAction() {
        return action;
    }
}