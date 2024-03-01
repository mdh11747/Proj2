package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;

public class myftpserver {
    private static Socket clientSock;
    private static PrintStream ps;
    private static HashMap<String, Boolean> commandTable = new HashMap<String, Boolean>();

    public static void main(String[] args) {
        try {
            boolean isClient = true;
            String nport = args[0];
            String tport = args[1];
            ServerSocket serverSock = new ServerSocket(Integer.parseInt(nport));
            while (true) {
                System.out.println("Waiting for client...");
                clientSock = serverSock.accept();
                ClientHandler clientHandler = new ClientHandler(serverSock, clientSock, isClient, commandTable);
                clientHandler.start();
            }

        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}