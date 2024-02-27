package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;

public class myftpserver {
    private static Socket clientSock;
    private static PrintStream ps;

    public static void main(String[] args) {
        try {
        String nport = args[0];
        String tport = args[1];
        ServerSocket serverSock = new ServerSocket(Integer.parseInt(nport));
        while (true) {
            System.out.println("Waiting for client...");
            clientSock = serverSock.accept();
            System.out.println("Client accepted");
            ClientHandler clientHandler = new ClientHandler(clientSock);
            clientHandler.start();
        }

        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}