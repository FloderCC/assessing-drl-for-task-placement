package org.workflowsim.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Writer;

/**
 * The TSPSocketRequest class handles sending a request to a server and receiving a response.
 * It implements the Runnable interface to allow the request to be executed in a separate thread.
 */
public class TSPSocketRequest implements Runnable {
    /**
     * The output buffer to send data to the server.
     */
    private Writer out;

    /**
     * The input buffer to receive data from the server.
     */
    private DataInputStream in;

    /**
     * The content to be sent to the server.
     */
    private String content;

    /**
     * The server response.
     */
    private volatile String response = null;

    /**
     * Gets the server response.
     *
     * @return the server response as a String.
     */
    public String getResponse() {
        return response;
    }

    /**
     * Constructs a new TSPSocketRequest with the specified output buffer, input buffer, and content.
     *
     * @param out the Writer to send data to the server.
     * @param in the DataInputStream to receive data from the server.
     * @param content the content to be sent to the server.
     */
    public TSPSocketRequest(Writer out, DataInputStream in, String content) {
        this.out = out;
        this.in = in;
        this.content = content;
    }

    /**
     * Sends the content to the server and reads the response.
     * This method is executed when the thread is started.
     */
    @Override
    public void run() {
        try {
            out.write(this.content);
            out.flush();
            response = in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}