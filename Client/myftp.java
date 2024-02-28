package Client;

import java.util.Arrays;
import java.util.Scanner;
import java.net.*;
import java.io.*;

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

            while (!command.equals("quit")) {
                System.out.print("mytftp>");
                input = scan.nextLine();
                input = input.trim();
                boolean threaded = input.endsWith("&");
                if (threaded) {input = input.substring(0,input.length() - 1); input = input.trim();}
                command = input.substring(0, input.contains(" ") ? input.indexOf(" ") : input.length());
                String inputArg = input.substring(input.contains(" ") ? input.indexOf(" ") + 1 : input.length());
                boolean contains = Arrays.stream(commands).anyMatch(command::equals);
                File clientFile;
                byte[] clientFileBytes;
                if (contains) {
                    if (threaded) {
                        input = input + "&";
                    }
                    out.writeUTF(input);
                    switch (command) {
                        case ("get"):
                        String message = "";
                            try {
                                message = in.readUTF();
                                String commandID = "";
                                if (message.endsWith("$")) {
                                    String temp = message.substring(message.indexOf("$") + 1, message.length() - 1);
                                    System.out.println("Command ID for file transfer is " + temp);
                                    commandID = temp;
                                    message = message.substring(0, message.indexOf("$"));
                                }
                                if (message.length() > 5 && message.substring(0, 5).equals("ERROR")) {
                                    System.out.println(message);
                                }
                            } catch (Exception e) {
                                System.out.println("There was an error");
                            }
                            final String tempMessage = message;
                            if (threaded) {
                                new Thread(() -> {
                                    handleGet(in, tempMessage);
                                }).start();
                            } else {
                                System.out.println(handleGet(in, tempMessage));
                            }
                            break;
                        case ("put"):
                            if (threaded) {
                                new Thread(() -> {
                                    handlePut(out, inputArg);
                                }).start();
                            } else {
                                System.out.println(handlePut(out, inputArg));
                            }
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
                                out.writeUTF(inputArg);
                                System.out.println("The delete command transferred to server successfully");
                                System.out.println(in.readUTF());
                            } catch (Exception e) {
                                System.out.println("There was an error deleting the file");
                            }
                            break;

                        case ("ls"):
                            try {
                                out.writeUTF(input);
                                String fileList = in.readUTF();
                                fileList = in.readUTF();
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
            System.out.println(e);
        }
    }

    public static String getFileFromArg(String arg) {
        return arg.substring(arg.indexOf("/") + 1);
    }

    public static String handleGet(DataInputStream in, final String message) {
       try {
            String fileName = getFileFromArg(message);
            FileOutputStream fos = new FileOutputStream("./Client/" + fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] bytes = new byte[10000];
            int fileLength = in.read(bytes);
            byte[] temp = new byte[fileLength];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = bytes[i];
            }
            fos.write(temp);
            fos.close();
            return "SUCCESS: File:" + fileName
                    + " was transferred from server to client successfully";
         } catch (Exception e) {
            return "Exception was reached: " + e;
        }
    }

    public static String handlePut(DataOutputStream out, String inputArg) {
        try {
            File clientFile = new File("./Client/" + inputArg);
            if (!clientFile.exists() || inputArg.equals("")) {
                System.out.println("There was an error transferring the fil");
                byte[] errorFile = new byte[3];
                out.write(errorFile, 0, 3);
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
        return "";
    }

}
