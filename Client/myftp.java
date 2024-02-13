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
                command = input.substring(0, input.contains(" ") ? input.indexOf(" ") : input.length());
                boolean threaded = command.endsWith("&");
                if (threaded) {command.substring(0,command.length() - 2);}
                String inputArg = input.substring(input.contains(" ") ? input.indexOf(" ") + 1 : input.length());
                boolean contains = Arrays.stream(commands).anyMatch(command::equals);
                File clientFile;
                byte[] clientFileBytes;
                if (contains) {
                    out.writeUTF(input);
                    switch (command) {

                        case ("get"):
                            String message = in.readUTF();
                            if (message.length() > 5 && message.substring(0, 5).equals("ERROR")) {
                                System.out.println(message);
                                continue;
                            }
                            String fileName = message;
                            fileName = getFileFromArg(fileName);
                            FileOutputStream fos = new FileOutputStream("./" + fileName);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            byte[] bytes = new byte[10000];
                            try {
                                int fileLength = in.read(bytes);
                                byte[] temp = new byte[fileLength];
                                for (int i = 0; i < temp.length; i++) {
                                    temp[i] = bytes[i];
                                }
                                fos.write(temp);
                                fos.close();
                                System.out.println("SUCCESS: File:" + fileName
                                        + " was transferred from server to client successfully");
                            } catch (Exception e) {
                                System.out.println("Exception was reached: " + e);
                            }
                            break;

                        case ("put"):
                            try {
                                clientFile = new File("./" + inputArg);
                                if (!clientFile.exists() || inputArg.equals("")) {
                                    System.out.println("There was an error transferring the fil");
                                    byte[] errorFile = new byte[3];
                                    out.write(errorFile, 0, 3);
                                } else {
                                        FileInputStream fis = new FileInputStream(clientFile);
                                        BufferedInputStream buffIn = new BufferedInputStream(fis);
                                        BufferedOutputStream buffOut = new BufferedOutputStream(sock.getOutputStream());
                                        byte []arr = new byte [1024 * 1024];
                                        int available  = -1;
                                        while((available = buffIn.read(arr)) > 0) {   
                                            buffOut.write(arr, 0, available); 
                                        }      
                                        buffOut.flush();
                                        buffOut.close();  
                                System.out.println("File transferred to server successfully");
                                }
                            } catch (Exception e) {
                                System.out.println("There was an error transferring the file");
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

}
