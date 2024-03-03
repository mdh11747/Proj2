package Client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.net.*;
import java.io.*;
import java.lang.Thread;

public class myftp {
    private static String input;
    private static HashMap<Long, Thread> activeThreads = new HashMap<>();
   // private static HashMap<Long, Thread> activeThreadsPut = new HashMap<>();
    private static int numThreadsOpened;

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        numThreadsOpened = 2;
        String sysName = args[0];
        String nPort = args[1];
        String tPort = args[2];
        int port = Integer.parseInt(args[1]);
        int terminatePort = Integer.parseInt(args[2]);
        String[] commands = { "get", "put", "delete", "ls", "cd", "mkdir", "pwd", "quit", "terminate" };

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
                System.out.print("myftp>");
                input = scan.nextLine();
                input = input.trim();
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
                        System.out.println("Wrote");
                    }
                    switch (command) {
                        case ("get"):
                            if (threaded) {
                                handleThread("get", sysName, port, terminatePort, inputArg);
                            } else {
                                System.out.println(handleGet(in, inputArg, threaded));
                            }
                            break;
                        case ("put"):
                            if (threaded) {
                                handleThread("put", sysName, port, terminatePort, inputArg);
                            } else {
                                System.out.println(handlePut(out, inputArg));
                            }
                            break;
                        case ("pwd"):
                            if (threaded) {
                                handleThread("pwd", sysName, port, terminatePort, inputArg);
                            } else {
                                System.out.println(br.readLine());
                            }
                            break;

                        case ("mkdir"):
                            if (threaded) {
                                handleThread("mkdir", sysName, port, terminatePort, inputArg);
                            } else {
                                System.out.println(br.readLine());
                            }
                            break;

                        case ("cd"):
                            if (threaded) {
                                handleThread("cd", sysName, port, terminatePort, inputArg);
                            } else {
                                System.out.println(br.readLine());
                            }
                            break;

                        case ("delete"):
                            if (threaded) {
                                handleThread("delete", sysName, port, terminatePort, inputArg);
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
                                handleThread("ls", sysName, port, terminatePort, inputArg);
                            } else {
                                try {
                                    out.writeUTF(input);
                                    String fileList = in.readUTF();
                                    fileList = in.readUTF();
                                    System.out.println(fileList);
                                } catch (Exception e) {
                                    System.out.println("There was an error listing the files");
                                }

                            }
                            break;

                        case ("terminate"):
                            long threadIdToTerminate = Long.parseLong(inputArg);
                            terminateThread(threadIdToTerminate, terminateOut);
                            break;
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

    public static String handleGet(DataInputStream in, String inputArg, boolean threaded) {
        try {
            
            long threadId = in.readLong();
            boolean isFile = in.readBoolean();
            if (!isFile) {
                return "File not found on server";
            }
            long fileSize = in.readLong();
            System.out.println(fileSize);
            if (fileSize < 0) {
                return "File not found or error occurred";
            }
            File targetFile = new File("./Client/" + inputArg);
            OutputStream fileOutStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalRead = 0;

            while (totalRead < fileSize && (bytesRead = in.read(buffer)) > 0) {
                fileOutStream.write(buffer, 0, bytesRead);
                if (!activeThreads.containsKey(Long.valueOf(threadId)) && threaded) {
                    fileOutStream.flush();
                    fileOutStream.close();
                    targetFile.delete();
                }
                totalRead += bytesRead;
            }
            System.out.println(totalRead);
            fileOutStream.flush();
            fileOutStream.close();
            return ("File " + inputArg + " received successfully.");
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
                System.out.println("FileSize: " + fileSize);

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

    private static void handleThread(String command, String sysName, int port, int terminatePort, String inputArg) {
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
                        System.out.print(handleGet(threadedIn, inputArg, true) + "\nmyftp>");
                        break;
                    case ("put"):
                        System.out.print(handlePut(threadedOut, inputArg) + "\nmyftp>");
                        break;
                    case ("pwd"):
                        System.out.print("\n" + br.readLine() + "\nmyftp>");
                        break;

                    case ("mkdir"):
                        System.out.print(br.readLine() + "\nmyftp>");
                        break;

                    case ("cd"):
                        System.out.print(br.readLine() + "\nmyftp>");
                        break;

                    case ("delete"):
                        try {
                            threadedOut.writeUTF(inputArg);
                            System.out.print("The delete command transferred to server successfully\nmyftp>");
                            System.out.print(threadedIn.readUTF() + "\nmyftp>");
                        } catch (Exception e) {
                            System.out.print("There was an error deleting the file\nmyftp>");
                        }
                        break;

                    case ("ls"):
                        try {
                            threadedOut.writeUTF(input);
                            String fileList = threadedIn.readUTF();
                            fileList = threadedIn.readUTF();
                            System.out.print("\n" + fileList + "\nmyftp>");
                        } catch (Exception e) {
                            System.out.print("There was an error listing the files\nmyftp>");
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
        int random = (int) (Math.random() * 1000);
        long threadId = thread.getId() + numThreadsOpened;
        activeThreads.put(threadId, thread);
        System.out.print("Thread id is " + threadId + "\n");
        numThreadsOpened++;
    }

    public static void terminateThread(long threadId, DataOutputStream terminateOut) {
        try {
            Thread thread = activeThreads.get(threadId);
            if (thread != null) {
                terminateOut.writeUTF("terminate " + threadId);
                thread.interrupt(); // Request the thread to stop
                activeThreads.remove(threadId); // Remove the thread from the map
                System.out.println("Thread " + threadId + " has been requested to terminate.");
            } else {
                System.out.println("Thread with ID " + threadId + " not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
