package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;

public class ClientHandler extends Thread {
    private String pwd = "./Server/";
    private Socket clientSock;
    private PrintStream ps;
    private HashMap<String, Trio> table;
    private HashMap<String, String> fileNameToThreadID;
    private boolean isClient;

    public ClientHandler(boolean isClient, Socket clientSock, HashMap<String, Trio> table, HashMap<String, String> fileNameToThreadID) {
        this.clientSock = clientSock;
        this.table = table;
        this.isClient = isClient;
        this.fileNameToThreadID = fileNameToThreadID;
    }

    public void run() {
        try {
            ps = new PrintStream(clientSock.getOutputStream());
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSock.getInputStream()));
            DataOutputStream outputStream = new DataOutputStream(clientSock.getOutputStream());
            String inputLine, inputArg, directArg, command;
            Boolean threaded;
            command = "";
            while (true) {
                System.out.println("Waiting for input");
                inputLine = in.readUTF();
                System.out.println(inputLine);
                if (!isClient) {
                if (inputLine.substring(0, 4).equals("$get")) {
                    String fileName = inputLine.substring(4, inputLine.indexOf("#"));
                    String comm = inputLine.substring(inputLine.indexOf("#") + 1);
                    table.put(comm, new Trio(false, fileName, "get"));
                    fileNameToThreadID.put(fileName, comm);
                } else if (inputLine.substring(0, 4).equals("$put")) {
                    String fileName = inputLine.substring(4, inputLine.indexOf("#"));
                    String comm = inputLine.substring(inputLine.indexOf("#") + 1);
                    table.put(comm, new Trio(false, fileName, "put"));
                    fileNameToThreadID.put(fileName, comm);
                }
            }
                System.out.println("Input: " + inputLine);
                threaded = inputLine.trim().charAt(inputLine.length() - 1) == '&';
                if (threaded) {
                    inputLine = inputLine.substring(0, inputLine.length() - 1);
                }
                command = inputLine.substring(0, inputLine.contains(" ") ? inputLine.indexOf(" ") : inputLine.length());
                inputArg = getFileFromArg(
                        inputLine.substring(inputLine.contains(" ") ? inputLine.indexOf(" ") + 1 : inputLine.length()));
                directArg = inputLine
                        .substring(inputLine.contains(" ") ? inputLine.indexOf(" ") + 1 : inputLine.length());
                String fileName = inputLine
                        .substring(inputLine.contains(" ") ? inputLine.indexOf(" ") + 1 : inputLine.length());
                switch (command) {
                    case ("get"):
                        while (fileNameToThreadID.containsKey(fileName)) {
                            
                        }
                        System.out.println("get command recognized");
                        if (!isClient) {
                            getFile(fileName, outputStream, threaded);
                            outputStream.writeBoolean(true);
                            return;
                        }
                        getFile(fileName, outputStream, threaded);
                        break;

                    case ("put"):
                        while (fileNameToThreadID.containsKey(fileName)) {

                        }
                        if (!isClient) {
                            putFile(inputArg, in, outputStream, threaded);
                            outputStream.writeBoolean(true);
                            return;
                        } else {
                            putFile(inputArg, in, outputStream, threaded);
                        }
                        break;

                    case ("delete"):
                        System.out.println("delete command recognized");
                        boolean worked = deleteFile(inputArg);

                        if (worked == true) {
                            outputStream.writeUTF("File successfully deleted");
                        } else {
                            outputStream.writeUTF("Error deleting file");
                        }
                        break;

                    case ("ls"):
                        System.out.println("ls command recognized");
                        File currDirectory = new File(getPwd());
                        File[] files = currDirectory.listFiles();
                        String rtn = "";
                        for (File file : files) {
                            rtn += file.getName();
                            rtn += " ";
                        }
                        outputStream.writeUTF(rtn);
                        break;

                    case ("cd"):
                        System.out.println("cd command recognized");
                        cd(inputArg);
                        break;

                    case ("mkdir"):
                        System.out.println("mkdir command recognized");
                        mkdir(directArg);
                        break;

                    case ("pwd"):
                        System.out.println("pwd command recognized");
                        pwd();
                        break;

                    case ("quit"):
                        System.out.println("quit command recognized");
                        clientSock.close();
                        break;

                    case ("terminate"):
                        final String finalInputArg = inputArg;
                        System.out.println(finalInputArg);
                        System.out.println(table);
                        new Thread(() -> {
                            try {
                                System.out.println("thread opened");
                                if (table.get(finalInputArg) == null) { 
                                    System.out.println("no exist");
                                    outputStream.writeUTF("Command Id doesn't exist");
                                } else if (table.get(finalInputArg).getStatus()) { 
                                   outputStream.writeUTF("file transfer already completed, terminate command didn't work");
                                } else { // delete on client
                                    if (table.get(finalInputArg).getGetOrPut().equals("get")) {
                                        handleTerminate(finalInputArg);
                                        outputStream.writeUTF("Successfully terminated file trasnfer for command id: " + finalInputArg);
                                    } else { // delete on server
                                        handleTerminate(finalInputArg);
                                        outputStream.writeUTF("Successfully terminated file trasnfer for command id: " + finalInputArg);
                                    }
                                }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        break;

                    default:
                        System.out.println("Command not recognized.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getFile(String fileName, DataOutputStream out, Boolean threaded) {
        try {
            File serverFile = new File(getPwd() + fileName);
            long fileSize = serverFile.length();
            out.writeLong(fileSize);

            FileInputStream fis = new FileInputStream(serverFile);
            BufferedInputStream buffIn = new BufferedInputStream(fis);
            byte[] buffer = new byte[8 * 1024];
            int bytesSent;
            boolean received = true;
            while ((bytesSent = buffIn.read(buffer)) > 0) {
                if (bytesSent % (8 * 1024 * 1) == 0) {
                    if (!isClient) {
                    Thread.sleep(10000);
                    }
                    if (threaded && table.get(fileNameToThreadID.get(fileName)) == null) {
                        received = false;
                        byte[] temp = new byte[2];
                        temp = "$$".getBytes();
                        buffIn.read(temp);
                        System.out.println("gonna delete it");
                        break;
                    } else {
                        //System.out.println(table.get(fileNameToThreadID.get(fileName)) == null);
                    }
                }
                out.write(buffer, 0, bytesSent);
            }
            fileNameToThreadID.remove(fileName);
            System.out.println("hiiiii");
            out.flush();
            table.put(fileNameToThreadID.get(fileName), new Trio(true, fileName, "get"));
            if (received) {
            System.out.println("File " + fileName + " sent successfully");
            } else {
                System.out.println("deleted");
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void putFile(String fileName, DataInputStream in, DataOutputStream out, Boolean threaded) {
        try {
            
            long fileSize = in.readLong(); // Expect the file size
            System.out.println("Read fileSize");
            File targetFile = new File(pwd + fileName);
            OutputStream fileOutStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalRead = 0;
            boolean received = true;
            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                fileOutStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (totalRead % (8 * 1024 * 10) == 0) {
                    if (!isClient) {
                        Thread.sleep(1);
                    }
                    if (threaded && table.get(fileNameToThreadID.get(fileName)) == null) {
                        deleteFile(fileName);
                        received = false;
                        break;
                    }
                }
            }
            table.put(fileNameToThreadID.get(fileName), new Trio(true, fileName, "put"));
            fileOutStream.flush();
            fileOutStream.close();
            if (received) {
            System.out.println("File " + fileName + " received successfully.");
            }
            
        } catch (Exception e) {
            System.out.println("Exception during file reception: " + e);
        }
    }

    public String getFileFromArg(String arg) {
        return arg.substring(arg.indexOf("/") + 1);
    }

    private void cd(String directory) {
        if (directory.equals("")) {
            pwd = "./";
            ps.println("pwd is now " + pwd);
        } else if (directory.equals(".") || directory.equals("./")) {
            ps.println("pwd is now " + pwd);
        } else {
            try {
                File check = new File(pwd + directory);
                if (!check.exists() && !directory.equals("~")) {
                    ps.println("Directory does not exist, please try again");
                } else {
                    if (directory.equals("~")) {
                        System.out.println(directory.length());
                        pwd = "./";
                    } else if (directory.length() == 1) {
                        pwd = pwd + directory + "/";
                    } else if (directory.substring(0, 2).equals("..")) {
                        File file = new File(pwd);
                        if (!pwd.equals("./")) {
                            pwd = file.getParent() + "/";
                        }
                    } else if (directory.substring(0, 1).equals(".")) {
                        pwd += directory.substring(2);
                    } else {
                        if (directory.substring(0, 1).equals("/")) {
                            directory = directory.substring(1);
                        }
                        pwd += directory;
                        if (!(pwd.charAt(pwd.length() - 1) == '/')) {
                            pwd += "/";
                        }
                    }
                    ps.println("pwd is now " + pwd);
                }
            } catch (Exception e) {
                System.out.print(e);
            }
        }

    }

    private void mkdir(String directory) {
        try {
            String[] forbidden = { "/", "\\", ":", "!", "*", "\"", "<", ">", "?", "." };
            if (Arrays.stream(forbidden).anyMatch(directory::contains)) {
                ps.println("Folder name not accepted, please try again");
            } else {
                File folder = new File(pwd + directory);
                if (folder.isDirectory()) {
                    ps.println("Directory already exists, please try again");
                } else {
                    folder.mkdirs();
                    byte[] serverFileBytes = new byte[(int) folder.length()];
                    clientSock.getOutputStream().write(serverFileBytes, 0, serverFileBytes.length);
                    ps.println(directory + " successfully created");
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String getPwd() {
        return pwd;
    }

    private void pwd() {
        System.out.println(pwd);
        ps.println(pwd);
    }

    public boolean deleteFile(String fileName) {
       File fileToDelete = new File(getPwd() + fileName);
        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
                System.out.println("File deleted successfully");
            } else {
                System.out.println("Unable to delete the file");
                return false;
            }
        }  else {
            System.out.println("File does not exist");
            return false;
        }
        return true;
    }

    public void handleTerminate(String inputArg) {
        table.remove(inputArg);
    }

}

