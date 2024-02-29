package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;

public class myftpserver {
    private static Socket clientSock;
    private static Socket tSock;
    private static PrintStream ps;
    private static HashMap<String, Pair> commandTable = new HashMap<String, Pair>(); // Pair allows knowing status and file name

    public static void main(String[] args) {
        try {
        String nport = args[0];
        String tport = args[1];
        ServerSocket serverSock = new ServerSocket(Integer.parseInt(nport));
        ServerSocket terminateSock = new ServerSocket(Integer.parseInt(tport));
        while (true) {
            System.out.println("Waiting for client...");
            clientSock = serverSock.accept();
            System.out.println("Client accepted");
            tSock = terminateSock.accept();
            System.out.println("Client accepted terminate port");
            ClientHandler clientHandler = new ClientHandler(clientSock, commandTable);
            clientHandler.start();
            ClientHandler tHandler = new ClientHandler(tSock, commandTable);
            tHandler.start();
        }

        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

class Pair {
    private Boolean status;
    private String fileName;
    
    Pair (Boolean status, String fileName) {
        this.status = status;
        this.fileName = fileName;
    }

    public Boolean getStatus() {
        return status;
    }

    public String getFileName() {
        return fileName;
    }
}