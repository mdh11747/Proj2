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
    private static HashMap<String, Trio> commandTable = new HashMap<String, Trio>(); 
    private static HashMap<String, String> fileNameToThreadID = new HashMap<String, String>();

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
            DataInputStream in = new DataInputStream(clientSock.getInputStream());
            boolean isClient = in.readBoolean();
            ClientHandler clientHandler = new ClientHandler(isClient, clientSock, commandTable, fileNameToThreadID);
            clientHandler.start();
            ClientHandler tHandler = new ClientHandler(isClient, tSock, commandTable, fileNameToThreadID);
            tHandler.start();
        }

        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

class Trio {
    private Boolean status;
    private String fileName;
    private String getOrPut;
    
    Trio (Boolean status, String fileName, String getOrPut) {
        this.status = status;
        this.fileName = fileName;
        this.getOrPut = getOrPut;
    }

    public Boolean getStatus() {
        return status;
    }

    public String getFileName() {
        return fileName;
    }
     public String getGetOrPut() {
        return getOrPut;
    }
}