package Client;

import java.util.Arrays;
import java.util.Scanner;
import java.net.*;
import java.io.*;
import java.lang.Thread;
import java.util.Random;

public class myftp {
    private static String input;

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String sysName = args[0];
        String nPort = args[1];
        String tPort = args[2];
        int port = Integer.parseInt(args[1]);
        int terminatePort = Integer.parseInt(args[2]);
        String[] commands = { "get", "put", "delete", "ls", "cd", "mkdir", "pwd", "quit" };

        try {
            Socket sock = new Socket(sysName, port);
            Socket tSock = new Socket(sysName, terminatePort);
            DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
            DataInputStream terminateIn = new DataInputStream(new BufferedInputStream(tSock.getInputStream()));
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            DataOutputStream terminateOut = new DataOutputStream(tSock.getOutputStream());
            out.writeBoolean(true);
            String command = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            BufferedReader terminateBr = new BufferedReader(new InputStreamReader(tSock.getInputStream()));
            String message = "";
            boolean threaded;
            while (!command.equals("quit")) {
                System.out.print("mytftp>");
                input = scan.nextLine();
                input = input.trim();
                String tempInput = input;
                threaded = input.endsWith("&");
                if (threaded) {
                    input = input.substring(0, input.length() - 1);
                    input = input.trim();
                }
                command = input.substring(0, input.contains(" ") ? input.indexOf(" ") : input.length());
                String inputArg = input.substring(input.contains(" ") ? input.indexOf(" ") + 1 : input.length());
                boolean contains = Arrays.stream(commands).anyMatch(command::equals);
                File clientFile;
                byte[] clientFileBytes;
                if (contains) {
                    if (threaded) {
                        input = input + "&";
                    } else {
                        out.writeUTF(input);
                    }
                    switch (command) {
                        case ("get"):
                            if (threaded) {
                                handleThread("get", sysName, port, terminatePort, inputArg, out, input.substring(4, input.indexOf("&")));
                            } else {
                                System.out.println(handleGet(in, inputArg));
                            }
                            break;
                        case ("put"):
                            if (threaded) {
                                handleThread("put", sysName, port, terminatePort, inputArg, out, input.substring(4, input.indexOf("&")));
                            } else {
                                System.out.println(handlePut(out, inputArg));
                            }
                            break;
                        case ("pwd"):
                            if (threaded) {
                                handleThread("pwd", sysName, port, terminatePort, inputArg, out, "");
                            } else {
                                System.out.println(br.readLine());
                            }
                            break;

                        case ("mkdir"):
                            if (threaded) {
                                handleThread("mkdir", sysName, port, terminatePort, inputArg, out, "");
                            } else {
                                System.out.println(br.readLine());
                            }
                            break;

                        case ("cd"):
                            if (threaded) {
                                handleThread("cd", sysName, port, terminatePort, inputArg, out, "");
                            } else {
                                System.out.println(br.readLine());
                            }
                            break;

                        case ("delete"):
                            if (threaded) {
                                handleThread("delete", sysName, port, terminatePort, inputArg, out, "");
                            } else {
                                try {
                                    out.writeUTF(inputArg);
                                    System.out.println("The delete command transferred to server successfully");
                                    System.out.println(in.readUTF());
                                } catch (Exception e) {
                                    System.out.println("There was an error deleting the file");
                                }
                            }
                            break;

                        case ("ls"):
                            if (threaded) {
                                handleThread("ls", sysName, port, terminatePort, inputArg, out, "");
                            } else {
                                try {
                                    out.writeUTF(input);
                                    String fileList = in.readUTF();
                                    fileList = in.readUTF();
                                    System.out.println(fileList);
                                } catch (Exception e) {
                                    System.out.println("There was an error listing the files");
                                }
                                break;
                            }
                    }
                } else if (command.equalsIgnoreCase("terminate")) {
                    terminateOut.writeUTF(input);
                    String status = terminateIn.readUTF();
                    if (status.charAt(0) == '$') { // terminate get command
                        File fileToDelete = new File("Client/" + status.substring(1));
                        if (fileToDelete.exists()) {
                            if (fileToDelete.delete()) {
                                System.out.println("File transfer terminated successfully");
                            } else {
                                System.out.println("Unable to delete the file");
                            }
                        } else {
                            System.out.println(status.substring(1));
                        }
                    } else {
                        System.out.println(status.substring(1));
                    }
                } else {
                    System.out.println("Command not recognized");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFileFromArg(String arg) {
        return arg.substring(arg.indexOf("/") + 1);
    }

    public static String handleGet(DataInputStream in, String inputArg) {
        try {
            long fileSize = in.readLong();
            if (fileSize < 0) {
                return "File not found or error occurred";
            }
            File targetFile = new File("./Client/" + inputArg);
            OutputStream fileOutStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalRead = 0;
            boolean received = true;
            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                 if (bytesRead < 6) {
                    targetFile.delete();
                    received = false;
                    break;
                }
                fileOutStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            fileOutStream.flush();
            fileOutStream.close();
            if (received) {
                return ("File " + inputArg + " received successfully.");
            } else {
                return "Deleted";
            }
        } catch (Exception e) {
            return "Exception was reached: " + e;
        }
    }

    public static String handlePut(DataOutputStream out, String inputArg) {
        try {
            File clientFile = new File("./Client/" + inputArg);
            if (!clientFile.exists() || inputArg.equals("")) {
                byte[] errorFile = new byte[3];
                out.write(errorFile, 0, 3);
                return ("There was an error transferring the fil");
            } else {
                FileInputStream fis = new FileInputStream(clientFile);
                BufferedInputStream buffIn = new BufferedInputStream(fis);
                long fileSize = clientFile.length();
                out.writeLong(fileSize); // Send the file size first

                byte[] arr = new byte[8 * 1024];
                int count;
                while ((count = buffIn.read(arr)) > 0) {
                    out.write(arr, 0, count);
                }
                out.flush(); // Ensure all data is sent
                return "File transferred to server successfully";
            }
        } catch (Exception e) {
            return "There was an error transferring the file";
        }
    }

    private static void handleThread(String command, String sysName, int port, int terminatePort, String inputArg, DataOutputStream out, String fileName) {
        Thread thread = new Thread(() -> {
            try {
                Socket threadedSock = new Socket(sysName, port);
                Socket threadedTerm = new Socket(sysName, terminatePort);
                BufferedReader br = new BufferedReader(new InputStreamReader(threadedSock.getInputStream()));
                BufferedReader terminateBr = new BufferedReader(new InputStreamReader(threadedTerm.getInputStream()));
                DataOutputStream threadedOut = new DataOutputStream(threadedSock.getOutputStream());
                threadedOut.writeBoolean(false);
                DataInputStream threadedIn = new DataInputStream(
                        new BufferedInputStream(threadedSock.getInputStream()));
                threadedOut.writeUTF(input.substring(0, input.length() - 1));
                switch (command) {
                    case ("get"):
                        System.out.println(handleGet(threadedIn, inputArg));
                        break;
                    case ("put"):
                        handlePut(threadedOut, inputArg);
                        break;
                    case ("pwd"):
                        System.out.println(br.readLine());
                        break;

                    case ("mkdir"):
                        System.out.println(br.readLine());
                        break;

                    case ("cd"):
                        System.out.println(br.readLine());
                        break;

                    case ("delete"):
                        try {
                            threadedOut.writeUTF(inputArg);
                            System.out.println("The delete command transferred to server successfully");
                            System.out.println(threadedIn.readUTF());
                        } catch (Exception e) {
                            System.out.println("There was an error deleting the file");
                        }
                        break;

                    case ("ls"):
                        try {
                            threadedOut.writeUTF(input);
                            String fileList = threadedIn.readUTF();
                            fileList = threadedIn.readUTF();
                            System.out.println(fileList);
                        } catch (Exception e) {
                            System.out.println("There was an error listing the files");
                        }
                        break;
                }
                threadedIn.readBoolean();
                threadedSock.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        int rand = 0;
        try {
        Random random = new Random();
        rand = random.nextInt(1000);
        out.writeUTF("$" + command + fileName + "#" + thread.getId() + Integer.toString(rand));
        } catch (Exception e) {

        }
        System.out.println("Thread id is " + thread.getId() + Integer.toString(rand));
    }

}
