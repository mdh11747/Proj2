package Client;

import java.net.*;
import java.io.*;

class ClientThread implements Runnable {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final String serverAddress;
    private final int port;

    public ClientThread(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public void run() {
        try {
            socket = new Socket(serverAddress, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            // Send a unique identifier or request for a dedicated thread
            out.writeUTF("Start Dedicated Thread");

            // Communication loop
            while (!Thread.currentThread().isInterrupted()) {
                // Send and receive messages
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
