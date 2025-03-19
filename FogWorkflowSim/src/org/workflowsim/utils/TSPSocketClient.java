package org.workflowsim.utils;

import com.mathworks.toolbox.javabuilder.external.org.json.JSONArray;
import com.mathworks.toolbox.javabuilder.external.org.json.JSONException;
import com.mathworks.toolbox.javabuilder.external.org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Client socket for the TSP simulation. Contains the methods for transfer information with the selected strategy code in another service
 *
 * @author Julio Corona
 * @since WorkflowSim Toolkit 1.0 's TSP extension
 */
public class TSPSocketClient {
    // initialize socket and input output streams

    /**
     * The socket connection
     */
    private static Socket socket;

    /**
     * The output buffer
     */
    private static Writer out;

    /**
     * The input buffer
     */
    private static DataInputStream in;

    /**
     * Open the connection to the server
     * @param address the server' ip address
     * @param port the server' port address
     */
    public static void openConnection(String address, int port){
        try {
            socket = new Socket(address, port);

            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Sends to the strategy algorithm the properties of the servers properties
     * @param cloudNodeFeatures the cloud server's properties
     * @param fogNodesFeatures the fog server's properties
     * @return the number of the server to place the task
     */
    public static String sendSeversSetup(String setupName,String strategy, double[] cloudNodeFeatures, double[][] fogNodesFeatures, int priorities_quantity, int randomSeed, Boolean drlLoadPretrainedModelOn, Boolean drlTrainingOn, Boolean drlSaveFinalModelOn, int cloudNodesUploadBandwidth, int cloudNodesDownloadBandwidth, int fogNodesUploadBandwidth, int fogNodesDownloadBandwidth, double latencyGatewayFogNode, double latencyGatewayCloudNode){
        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "setup");

            JSONObject data = new JSONObject();
            if (cloudNodeFeatures != null){
                data.put("cloud", new JSONArray(cloudNodeFeatures));
            }
            data.put("fog", new JSONArray(fogNodesFeatures));
            data.put("setup_name", setupName);

            data.put("strategy", strategy);
            data.put("random_seed", randomSeed);


            data.put("cloud_nodes_upload_bandwidth", cloudNodesUploadBandwidth);
            data.put("cloud_nodes_download_bandwidth", cloudNodesDownloadBandwidth);
            data.put("fog_nodes_upload_bandwidth", fogNodesUploadBandwidth);
            data.put("fog_nodes_download_bandwidth", fogNodesDownloadBandwidth);
            data.put("latency_gateway_fog_node", latencyGatewayFogNode);
            data.put("latency_gateway_cloud_node", latencyGatewayCloudNode);


            data.put("priorities_quantity", priorities_quantity);

            if (strategy.contains("DRL")){
                if (drlLoadPretrainedModelOn != null){
                    data.put("load_pretrained_model_on", drlLoadPretrainedModelOn);
                }

                if (drlTrainingOn != null){
                    data.put("training_on", drlTrainingOn);
                }

                if (drlSaveFinalModelOn != null){
                    data.put("save_final_model_on", drlSaveFinalModelOn);
                }
            }

            json.put("data", data);

            return makeRequest(json.toString());
        }
        catch (JSONException i) {
            i.printStackTrace();
        }
        return "ERROR";
    }

    /**
     * Ask the strategy algorithm for the selected option
     * @param state the simulation's environment information
     * @return the action to be done
     */
    public static TSPDecisionResult askForDecision(Long[] state){

        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "ask_decision");

            JSONObject data = new JSONObject();
            data.put("action_id", JSONObject.NULL);
            data.put("state", new JSONArray(state));

            json.put("data", data);

            // waiting for the response
            String content= makeRequest(json.toString());

            return TSPEnvHelper.parseStrArrayToTSPDecisionResult(content);
        }
        catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Ask the strategy algorithm for the selected option
     * @param state the simulation's environment information
     * @param action_id the action id
     * @return the action to be done
     */
    public static TSPDecisionResult askForDecisionWithActionId(int action_id, Double[] state){
        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "ask_decision");

            JSONObject data = new JSONObject();
            data.put("action_id", action_id);
            data.put("state", new JSONArray(state));

            json.put("data", data);

            // waiting for the response
            String content= makeRequest(json.toString());

            return TSPEnvHelper.parseStrArrayToTSPDecisionResult(content);
        }
        catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Save the placement reward
     * @param action_id the action id
     * @param reward the reward for RL agents
     * @return the server result
     */
    public static String saveReward(int action_id, Double reward){

        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "save_reward");

            JSONObject data = new JSONObject();
            data.put("action_id", action_id);

//            System.out.println("reward: "+reward);
            data.put("reward", reward);

            json.put("data", data);

            // waiting for the response
            return makeRequest(json.toString());
        }
        catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Send the new state for retraining the RL model
     * @param action_id the action id
     * @param state the simulation's environment information
     * @return the number of the server to place the task
     */

    public static String retrain(int action_id, Double[] state){
        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "retrain");

            JSONObject data = new JSONObject();
            data.put("action_id", action_id);
            data.put("state", new JSONArray(state));

            json.put("data", data);

            return makeRequest(json.toString());
        }
        catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Plot a value series
     * @param values the values to be plotted
     */
    public static <T extends Number> void plot(String plot_name, ArrayList<T> values){
        try {
            // saving the list to csv
            String filePath = "results_tsp/" + plot_name + ".csv";

            try (FileWriter writer = new FileWriter(filePath)) {
                // Write each value to the file, separating by commas or new lines
                int index = 1;

                for (T value : values) {
                    writer.append(index + "," + value);
                    writer.append("\n"); // You can use "\n" for new line instead of a comma
                    index++;
                }
                System.out.println("ArrayList saved to CSV successfully.");
            } catch (IOException e) {
                System.out.println("An error occurred while saving the ArrayList to CSV.");
                e.printStackTrace();
            }

            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "plot");
            json.put("plot_name", plot_name);

            json.put("data", new JSONArray(values));

//            makeRequest(json.toString());
        }
        catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
        }
    }

    public static void plot(String plot_name, HashMap<Integer, Double> values){
        // saving the list of index, value to csv
        String filePath = "results_tsp/" + plot_name + ".csv";

        // Use FileWriter and BufferedWriter to write to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.newLine();

            // Iterate through the HashMap and write each entry
            for (Map.Entry<Integer, Double> entry : values.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();  // Handle exception
        }
    }

    /**
     * Saves the RL model to a file
     */
    public static void saveModel(int episode_number) {
        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "save_model");
            json.put("episode_number", episode_number);

            makeRequest(json.toString());
        }
        catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Go to the next episode
     */
    public static void nextEpisode() {
        try {
            // sends the info to the socket
            JSONObject json = new JSONObject();
            json.put("action", "next_episode");

            makeRequest(json.toString());
        } catch (JSONException i) {
            i.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Close the connection with the server
     */
    public static void closeConnection(){
        // close the connection
        try {
            out.close();
            in.close();
            socket.close();
            System.out.println("Disconnected");
        }
        catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * make the request with failure check
     */
    private static String makeRequest(String content) {
        // sending the information
        try {
            content = content + "\n";

            TSPSocketRequest tsp_socket_request = new TSPSocketRequest(out, in, content);
            Thread thread = new Thread(tsp_socket_request);
            thread.start();

            int retry_count = 1;
            do{
                thread.join();

                String response = tsp_socket_request.getResponse();

                if (response != null){
                    return response;
                }else {
                    thread.interrupt();
                    thread = new Thread(tsp_socket_request);
                    thread.start();
                    retry_count += 1;

                }
            }while (retry_count <= 30);

            if (retry_count == 31){
                System.err.println("The maximum number of connection retries has been reached");
                System.exit(1);
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("There was an exception sending information");
            System.exit(1);
            return null;
        }
    }
}
