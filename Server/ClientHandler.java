package Server;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.Thread;
import java.util.HashMap;
import java.lang.Thread;

public class ClientHandler extends Thread {
    private String pwd = "./Server/";
    private Socket clientSock;
    private PrintStream ps;
    private HashMap<String, Pair> table;

    public ClientHandler(Socket clientSock, HashMap<String, Pair> table) {
        this.clientSock = clientSock;
        this.table = table;
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
                inputLine = in.readUTF();
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
                        if (threaded) {
                            System.out.println("put recognized");
                            final String finalInputArg = inputArg;
                            final boolean finalThreaded = threaded;
                            new Thread(() -> {
                                System.out.println("thread opened");
                                putFile(finalInputArg, in, outputStream, finalThreaded);
                                System.out.println("put file done");
                                try {
                                    outputStream.writeBoolean(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        } else {
                            putFile(inputArg, in, outputStream, threaded);
                            outputStream.writeBoolean(true);
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
                        System.out.println(table);
                        new Thread(() -> {
                            try {
                                System.out.println("thread opened");
                                if (table.get(finalInputArg) == null) { 
                                    outputStream.writeUTF("Command Id doesn't exist");
                                } else if (table.get(finalInputArg).getStatus()) { 
                                   outputStream.writeUTF("file transfer already completed, terminate command didn't work");
                                } else {
                                    handleTerminate(finalInputArg);
                                    outputStream.writeUTF("Successfully terminated file trasnfer for command id: " + finalInputArg);
                                }
                                    outputStream.writeBoolean(true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();
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
                table.put(commandID, new Pair(false, fileName));
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
                table.put(commandID, new Pair(true, fileName));
            }

            System.out.println("File " + fileName + " sent successfully");
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void putFile(String fileName, DataInputStream in, DataOutputStream out, Boolean threaded) {
        try {
            String commandID = "";
            if (threaded) {
                commandID = generateCommandID();
                table.put(commandID, new Pair(false, fileName));
                out.writeUTF(commandID);
            } else {
                out.writeUTF("ignore");
            }
            long fileSize = in.readLong(); // Expect the file size
            File targetFile = new File(pwd + fileName);
            OutputStream fileOutStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalRead = 0;
            Thread.sleep(1000); // 15 seconds
            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                fileOutStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            fileOutStream.flush();
            fileOutStream.close();
            if (threaded) {
                System.out.println("hello");
                table.put(commandID, new Pair(true, fileName));
            }
            System.out.println("File " + fileName + " received successfully.");
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

    public void handleTerminate(String inputArg) {
        deleteFile(table.get(inputArg).getFileName());
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

