package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;
import java.util.Set;

public class ClientHandler extends Thread {
    private String pwd = "./Server/";
    private Socket clientSock;
    private PrintStream ps;
    private HashMap<String, Trio> table;
    private boolean isClient;
    private HashMap<String, Boolean> isTerminated;

    public ClientHandler(boolean isClient, Socket clientSock, HashMap<String, Trio> table,
            HashMap<String, Boolean> isTerminated) {
        this.clientSock = clientSock;
        this.table = table;
        this.isClient = isClient;
        this.isTerminated = isTerminated;
    }

    public void run() {
        try {
            if (!isClient) {
                System.out.println("Activated");
                isTerminated.put(String.valueOf(Thread.currentThread().getId()), false);
            }
            ps = new PrintStream(clientSock.getOutputStream());
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSock.getInputStream()));
            DataOutputStream outputStream = new DataOutputStream(clientSock.getOutputStream());
            String inputLine, inputArg, directArg, command;
            Boolean threaded;
            command = "";
            while (true) {
                System.out.println("Waiting for input");
                inputLine = in.readUTF();
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
                        System.out.println("get command recognized");
                        getFile(fileName, outputStream, threaded);
                        sleep(1);
                        if (!isClient) {
                            outputStream.writeBoolean(true);
                            return;
                        }
                        break;

                    case ("put"):
                        putFile(inputArg, in, outputStream, threaded);
                        if (!isClient) {
                            outputStream.writeBoolean(true);
                            return;
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
                        isTerminated.put(directArg, true);
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
            out.writeLong(Thread.currentThread().getId());
            if (serverFile.exists()) {
                out.writeBoolean(true);
            } else {
                out.writeBoolean(false);
                return;
            }
            long fileSize = serverFile.length();
            out.writeLong(fileSize);

            FileInputStream fis = new FileInputStream(serverFile);
            BufferedInputStream buffIn = new BufferedInputStream(fis);
            byte[] buffer = new byte[8 * 1024];
            int bytesSent;
            while ((bytesSent = buffIn.read(buffer)) > 0) {
                if (isTerminated.size() > 0 && isTerminated.containsKey(String.valueOf(Thread.currentThread().getId())) && isTerminated.get(String.valueOf(Thread.currentThread().getId()))) {
                    //DataOutputStream termOut = new DataOutputStream(new BufferedInputStream(clientSock.getInputStream()));
                    System.out.println("Terminated");
                    out.flush();
                    return;
                }
                out.write(buffer, 0, bytesSent);
            }
            out.flush();

            System.out.println("File " + fileName + " sent successfully");
        } catch (Exception e) {
            e.printStackTrace();
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
            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                System.out.println(Thread.currentThread().getId());
                if (isTerminated.size() > 0 && isTerminated.get(String.valueOf(Thread.currentThread().getId()))) {
                    System.out.println("Terminated");
                    fileOutStream.flush();
                    fileOutStream.close();
                    deleteFile(fileName);
                    return;
                }
                
                fileOutStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            fileOutStream.flush();
            fileOutStream.close();
            if (threaded) {
                table.put("rawr", new Trio(true, fileName, "get"));
            }
            System.out.println("File " + fileName + " received successfully.");

        } catch (Exception e) {
            e.printStackTrace();
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

    public void handleTerminate(String inputArg) {
        deleteFile(table.get(inputArg).getFileName());
    }

}
