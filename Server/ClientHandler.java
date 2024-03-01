package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;

public class ClientHandler extends Thread {
    private String pwd = "./Server/";
    private Socket clientSock;
    private ServerSocket serverSock;
    private PrintStream ps;
    private HashMap<String, Boolean> table;
    private boolean isClient;

    public ClientHandler(ServerSocket serverSock, Socket clientSock, boolean isClient, HashMap<String, Boolean> table) {
        this.clientSock = clientSock;
        this.serverSock = serverSock;
        this.table = table;
        this.isClient = isClient;
    }

    public void run() {
        try {
            System.out.println("ClientHandler Opened");
            ps = new PrintStream(clientSock.getOutputStream());
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSock.getInputStream()));
            DataOutputStream outputStream = new DataOutputStream(clientSock.getOutputStream());
            String inputLine = "", inputArg, directArg, command;
            Boolean threaded;
            command = "";
            while (true) {
                System.out.println("Input reached");
                System.out.println("Waiting for server input");
                inputLine = in.readUTF();
                System.out.println(inputLine);
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
                        System.out.println("get command recognized");
                        getFile(fileName, outputStream, threaded);
                        break;

                    case ("put"):
                        System.out.println("put recognized");

                        if (threaded) {
                            putFile(inputArg, in, outputStream);
                            try {
                                System.out.println("pre-close");
                                clientSock.close();
                                System.out.println("Socket closed");
                                return;
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        } else {
                            putFile(inputArg, in, outputStream);
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
                        break;

                    default:
                        System.out.println("Command not recognized.");
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void getFile(String fileName, DataOutputStream out, Boolean threaded) {
        try {
            String commandID = "";
            if (threaded) {
                commandID = generateCommandID();
                table.put(commandID, false);
                out.writeUTF(commandID);
            } else {
                out.writeUTF("ignore");
            }
            File serverFile = new File(getPwd() + fileName);
            long fileSize = serverFile.length();
            out.writeLong(fileSize);

            FileInputStream fis = new FileInputStream(serverFile);
            BufferedInputStream buffIn = new BufferedInputStream(fis);
            byte[] buffer = new byte[8 * 1024];
            int bytesSent;
            while ((bytesSent = buffIn.read(buffer)) > 0) {
                out.write(buffer, 0, bytesSent);
            }
            out.flush();

            if (threaded) {
                table.put(commandID, true);
            }

            System.out.println("File " + fileName + " sent successfully");
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void putFile(String fileName, DataInputStream in, DataOutputStream out) {
        try {
            System.out.println("Waiting for filesize");
            long fileSize = in.readLong(); // Expect the file size
            File targetFile = new File(pwd + fileName);
            OutputStream fileOutStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalRead = 0;
            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                fileOutStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            fileOutStream.flush();
            fileOutStream.close();
            System.out.println("File " + fileName + " received successfully.");
            System.out.println("Writing Ignore");
            out.writeUTF("ignore");
            System.out.println("Ignore wrote");
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
        } else {
            System.out.println("File does not exist");
            return false;
        }
        return true;
    }

    public String generateCommandID() {
        for (int i = 1000; i < 9999; i++) {
            if (!table.containsKey(Integer.toString(i))) {
                return Integer.toString(i);
            }
        }
        return "9999";
    }
}
