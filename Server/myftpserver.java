package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;

public class myftpserver {

    private static String pwd = "./";
    private static Socket clientSock;
    private static PrintStream ps;

    public static void main(String[] args) {
        String nport = args[0];
        String tport = args[1];
        try {
            ServerSocket serverSock = new ServerSocket(Integer.parseInt(nport));
            System.out.println("Waiting for client...");
            clientSock = serverSock.accept();
            System.out.println("Client accepted");
            ps = new PrintStream(clientSock.getOutputStream());
            DataInputStream in = new DataInputStream(new BufferedInputStream(clientSock.getInputStream()));
            DataOutputStream outputStream = new DataOutputStream(clientSock.getOutputStream());
            String inputLine, inputArg, directArg, command;
            command = "";
            while (true) {
                inputLine = in.readUTF();
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
                        getFile(fileName, clientSock, outputStream);
                        break;

                    case ("put"):
                        System.out.println("put command recognized");
                        putFile(inputArg, in, outputStream);
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
                        serverSock.close();
                        serverSock = new ServerSocket(Integer.parseInt(nport));
                        System.out.println("Waiting for client...");
                        clientSock = serverSock.accept();
                        System.out.println("Client accepted");
                        ps = new PrintStream(clientSock.getOutputStream());
                        in = new DataInputStream(new BufferedInputStream(clientSock.getInputStream()));
                        outputStream = new DataOutputStream(clientSock.getOutputStream());
                        command = "";
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

    public static void getFile(String fileName, Socket sock, DataOutputStream out) {
        try {
            //DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            try {
                File serverFile = new File(getPwd() + fileName);
                byte[] serverFileBytes = new byte[(int) serverFile.length()];
                FileInputStream fis = new FileInputStream(serverFile);
                fis.read(serverFileBytes);
                out.writeUTF(fileName);
                out.write(serverFileBytes, 0, serverFileBytes.length);
                System.out.println("Succesfully sent file to client");
            } catch (Exception e) {
                out.writeUTF("ERROR: " + e);
                System.out.println(e);
            }
        } catch (Exception e) {
            System.out.println("Exception was reached");
        }
    }

    public static void putFile(String fileName, DataInputStream in, DataOutputStream out) {
        try {
            BufferedInputStream bis = new BufferedInputStream(clientSock.getInputStream());
            File targetFile = new File("./" + fileName);
            OutputStream outStream = new FileOutputStream(targetFile);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            bis.close();
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            System.out.println("Exception was reached: " + e);
        }
    }

    public static String getFileFromArg(String arg) {
        return arg.substring(arg.indexOf("/") + 1);
    }

    private static void cd(String directory) {
        if (directory.equals("")) {
            pwd = "./";
            ps.println("pwd is now " + pwd);
        }
        else if (directory.equals(".") || directory.equals("./")) {
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

    private static void mkdir(String directory) {
        try {
            String[] forbidden = { "/", "\\", ":", "!", "*", "\"", "<", ">", "?", "."};
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

    private static String getPwd() {
        return pwd;
    }

    private static void pwd() {
        System.out.println(pwd);
        ps.println(pwd);
    }

    public static boolean deleteFile(String fileName) {
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
}