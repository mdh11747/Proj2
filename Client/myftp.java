package Client;

import java.util.Arrays;
import java.util.Scanner;
import java.net.*;
import java.io.*;
import java.lang.Thread;

public class myftp {
    private static String input;

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String sysName = args[0];
        String nPort = args[1];
        String tPort = args[2];
        int port = Integer.parseInt(args[1]);
        String[] commands = { "get", "put", "delete", "ls", "cd", "mkdir", "pwd", "quit", "terminate" };
        try {
            Socket sock = new Socket(sysName, port);
            DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            String command = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String message = "";
            while (!command.equals("quit")) {
                System.out.print("mytftp>");
                input = scan.nextLine();
                input = input.trim();
                boolean threaded = input.endsWith("&");
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
                    }
                    System.out.println("Writing Input");
                    out.writeUTF(input);
                    System.out.println("Input Wrote");
                    switch (command) {
                        case ("get"):
                            if (threaded) {
                                long serverId = 0;
                                Thread thread = new Thread(() -> {
                                    handleGet(in, inputArg);
                                    try {
                                        in.readBoolean();
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    System.out.println("Client Thread Closed");
                                });
                                thread.start();
                            } else {
                                System.out.println(handleGet(in, inputArg));
                            }
                            break;
                        case ("put"):
                            if (threaded) {
                                Thread clientThread = new Thread(() -> {
                                    try {
                                        System.out.println("Thread opened");
                                        Socket threadSocket = new Socket(sysName,port);
                                        DataInputStream threadIn = new DataInputStream(new BufferedInputStream(threadSocket.getInputStream()));
                                        DataOutputStream threadOut = new DataOutputStream(threadSocket.getOutputStream());
                                        threadOut.writeUTF(input.substring(0,input.length() - 1));
                                        handlePut(threadOut, inputArg);
                                        //threadIn.readBoolean();
                                        threadSocket.close();
                                    } catch (Exception e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                });
                                clientThread.start();
                            } else {
                                System.out.println(handlePut(out, inputArg));
                            }
                            break;
                        case ("pwd"):
                        System.out.println("Reading line");
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
                                out.writeUTF(inputArg);
                                System.out.println("The delete command transferred to server successfully");
                                System.out.println(in.readUTF());
                            } catch (Exception e) {
                                System.out.println("There was an error deleting the file");
                            }
                            break;

                        case ("ls"):
                            try {
                                System.out.println("Writing ls");
                                out.writeUTF(input);
                                System.out.println("Read ls 1");
                                String fileList = in.readUTF();
                                System.out.println("Read ls 2");
                                fileList = in.readUTF();
                                System.out.println("Read done");
                                System.out.println(fileList);
                            } catch (Exception e) {
                                System.out.println("There was an error listing the files");
                            }
                            break;

                        case ("terminate"):
                            break;
                    }
                } else {
                    System.out.println("Command not recognized, try again");
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
            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                fileOutStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
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
                System.out.println("Filesize wrote");
                byte[] arr = new byte[8 * 1024];
                int count;
                while ((count = buffIn.read(arr)) > 0) {
                    
                    out.write(arr, 0, count);
                }
                System.out.println("while");
                out.flush(); // Ensure all data is sent
                return "File transferred to server successfully";
            }
        } catch (Exception e) {
            return "There was an error transferring the file";
        }
    }

}
