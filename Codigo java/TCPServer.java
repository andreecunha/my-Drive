package sockets;

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TCPServer {

    public static void main(String args[]) {


        if (args.length <3) {
            System.out.println("Invalid arguments");
            System.exit(0);

        }

        else {

            int[] data = getInfo(args[2]);
            String userFile = args[1];
            int serverPort = data[0];
            final int maxfailedrounds = data[1];
            final int timeout = data[2];
            final int bufsize = data[3];
            final int port = data[4];
            final int period = data[5];
            final int port2 = data[6];
            final int port3 = data[7];
            final int portDownloadUDP = data[8];

            if (args[0].equals("primary")) {

                int numero = 0;

                try (ServerSocket listenSocket = new ServerSocket(serverPort)) {

                    System.out.println("A escuta no porto " + serverPort);
                    System.out.println("LISTEN SOCKET=" + listenSocket);

                    Heartbeat heart1 = new Heartbeat(args[0], maxfailedrounds, timeout, bufsize, port, period, port2, port3, portDownloadUDP);
                    Thread h2 = new Thread(heart1);
                    h2.start();

                    while (true) {

                        Socket clientSocket = listenSocket.accept();
                        System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                        numero++;

                        new Connection(clientSocket, numero, userFile, portDownloadUDP);

                    }

                } catch (IOException e) {
                    System.out.println("Listen:" + e.getMessage());
                }

            } else if (args[0].equals("secondary")) {

                Heartbeat heart = new Heartbeat(args[0], maxfailedrounds, timeout, bufsize, port, period, port2, port3, portDownloadUDP);
                Thread h1 = new Thread(heart);
                h1.start();

                Heartbeat files = new Heartbeat("files", maxfailedrounds, timeout, bufsize, port, period, port2, port3, portDownloadUDP);
                Thread f = new Thread(files);
                f.start();

            } else
                System.out.println("Invalid arguments");
        }
        }

    public static int [] getInfo(String path) {

        int [] data = new int[9];
        File file = new File(path);
        int i=0;

        try {
            Scanner x = new Scanner(file);
            while (x.hasNextLine()) {
                String line = x.nextLine();
                String [] lineInfo = line.split(" ", 2);
                data[i] = Integer.valueOf(lineInfo[1]);
                i++;
            }

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        return data;

    }
}


class Connection extends Thread {

    DataInputStream in;
    static DataOutputStream out;
    Socket clientSocket;
    int thread_number;
    String userFile;
    int port3;
    DatagramSocket ds;

    public Connection(Socket aClientSocket, int numero, String userFile, int port3) {

        thread_number = numero;
        this.userFile = userFile;
        this.port3 = port3;

        clear(userFile);

        try {

            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            ds = new DatagramSocket(port3);
            this.start();


        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }


    public void run() {
        String[] clientInput;
        Boolean success;
        String strFiles;
        String strCwdServer;
        String[] files;
        String localPort = String.valueOf(clientSocket.getPort());


        try {
            while (true) {

                String data = in.readUTF();
                System.out.println("T[" + thread_number + "] Recebeu: " + data);
                clientInput = data.split(" ", 2);


                if (clientInput[0].equals("login")) {

                    String[] loginInfo = data.split(" ", 3);
                    success = login(loginInfo[1], loginInfo[2], localPort, userFile);

                    if (success) {
                        out.writeUTF("User logged in");
                    } else
                        out.writeUTF("Could not log in");
                } else if (getCwdServer(localPort, userFile).equals("False")) {
                    out.writeUTF("You are not logged in");


                } else {

                    if (clientInput[0].equals("changePassword")) {

                        String[] userInfo = data.split(" ", 4);
                        Boolean change = changePass(userInfo[1], userInfo[2], userInfo[3], userFile);
                        if (change)
                            out.writeUTF("Password changed successfully");
                        else
                            out.writeUTF("Could not change the password");


                    } else if (clientInput[0].equals("serverLs")) {


                        strFiles = null;
                        strCwdServer = getCwdServer(localPort, userFile);

                        files = ls(strCwdServer);
                        for (int i = 0; i < files.length; i++) {
                            strFiles += files[i] + "\n";
                        }

                        out.writeUTF(strFiles);

                    } else if (clientInput[0].equals("serverCd")) {

                        if (exists(clientInput[1])) {

                            if (hasAccess(clientInput[1], localPort, userFile)) {

                                cdServer(localPort, clientInput[1], userFile);

                            } else
                                out.writeUTF("Access denied");
                        } else
                            out.writeUTF("Directory does not exist");


                    } else if (clientInput[0].equals("pushFile")) {

                        if (exists(clientInput[1])) {

                            pushFile(in, localPort, userFile, ds);

                            out.writeUTF("File uploaded");

                        } else
                            out.writeUTF("File does not exist");


                    } else if (clientInput[0].equals("pullFile")) {

                        pullFile(clientInput[1]);

                    } else if (clientInput[0].equals("help")) {
                        out.writeUTF("Available commands:\n\nlogin user pass\nchangePassword user currentPassword newPassword\nclientLs (lists all files in the current directory)\nclientCd (changes the client's current directory)\nserverLs (lists all files in the server's current directory\nserverCd (changes the server's current directory)\npullFile path (downloads file from server)\npushFile path (uploads file to server)");


                    } else {
                        out.writeUTF("Unknown command - type 'help' to get the available commands");
                    }
                }
            }

        } catch (EOFException e) {
            System.out.println("EOF:" + e);
        } catch (IOException e) {
            System.out.println("IO:" + e);
        }
    }

    public static Boolean login(String user, String pass, String newPort, String path) {

        Boolean ret = false;
        File file = new File(path);
        File newFile = new File(concatString(path) + "tempFile.txt");

        try {
            FileWriter fw = new FileWriter(newFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            x = new Scanner(file);
            while (x.hasNext()) {
                String line = x.nextLine();
                String[] lineWords = line.split(" ", 4);

                if (lineWords[0].equals(user) && lineWords[1].equals(pass)) {
                    if (lineWords[2].equals("0")) {
                        pw.println(user + " " + pass + " " + newPort + " " + lineWords[3]);
                        ret = true;
                    } else
                        pw.println(user + " " + pass + " " + lineWords[2] + " " + lineWords[3]);
                } else {
                    pw.println(lineWords[0] + " " + lineWords[1] + " " + lineWords[2] + " " + lineWords[3]);
                }
            }

            x.close();
            pw.flush();
            pw.close();
            file.delete();
            File dump = new File(path);
            newFile.renameTo(dump);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return ret;
    }

    private static Scanner x;

    public static Boolean changePass(String User, String pass, String newPass, String userPath) {
        Boolean ret = false;
        File file = new File(userPath);
        File newFile = new File(concatString(userPath) + "temp_file.txt");
        try {
            FileWriter fw = new FileWriter(newFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            x = new Scanner(file);
            while (x.hasNext()) {
                String line = x.nextLine();
                System.out.println(line);
                String[] lineWords = line.split(" ", 4);

                if (lineWords[0].equals(User) && lineWords[1].equals(pass)) {
                    pw.println(User + " " + newPass + " " + lineWords[2] + " " + lineWords[3]);
                    ret = true;
                } else {
                    pw.println(lineWords[0] + " " + lineWords[1] + " " + lineWords[2] + " " + lineWords[3]);
                }
            }

            x.close();
            pw.flush();
            pw.close();
            file.delete();
            File dump = new File(userPath);
            newFile.renameTo(dump);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return ret;
    }

    public static String[] ls(String path) {
        Path aa = Paths.get(path);
        String[] files = new String[0];

        try (Stream<Path> subPaths = Files.walk(aa, 1)) {

            List<String> subPathList = subPaths.filter(Files::isRegularFile)
                    .map(Objects::toString)
                    .collect(Collectors.toList());

            files = new String[subPathList.size()];

            for (int i = 0; i < subPathList.size(); i++) {
                files[i] = subPathList.get(i);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return files;
    }

    public static Boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static Boolean hasAccess(String path, String localPort, String userPath) {

        File file = new File(userPath);
        String[] lineInfo;
        String username = " ";

        try {
            x = new Scanner(file);
            while (x.hasNextLine()) {
                String line = x.nextLine();
                lineInfo = line.split(" ", 4);
                if (lineInfo[2].equals(localPort)) {
                    username = lineInfo[0];
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        File fileUser = new File("/home/rodrigo/Desktop/SD_Projeto/home/users/" + username);

        String[] dirs = fileUser.list((dir, name) -> new File(dir, name).isDirectory());

        for (int i = 0; i < dirs.length; i++) {
            if (("/home/rodrigo/Desktop/SD_Projeto/home/users/" + username + "/" + dirs[i]).equals(path))
                return true;
        }
        return false;
    }


    public static String getCwdServer(String localPort, String pathUser) {

        File file = new File(pathUser);
        String[] lineInfo;
        try {
            x = new Scanner(file);
            while (x.hasNextLine()) {
                String line = x.nextLine();
                lineInfo = line.split(" ", 4);
                if (lineInfo[2].equals(localPort)) {
                    return lineInfo[3];
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return "False";
    }

    public static Boolean cdServer(String localPort, String newDir, String pathUser) {
        Boolean ret = false;
        File file = new File(pathUser);
        File newFile = new File(concatString(pathUser) + "tempFile.txt");
        String nome = "";
        String password = "";
        try {
            FileWriter fw = new FileWriter(newFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            x = new Scanner(file);
            while (x.hasNext()) {
                String line = x.nextLine();
                System.out.println(line);
                String[] lineWords = line.split(" ", 4);

                if (lineWords[2].equals(localPort)) {
                    pw.println(lineWords[0] + " " + lineWords[1] + " " + lineWords[2] + " " + newDir);
                    ret = true;
                } else {
                    pw.println(lineWords[0] + " " + lineWords[1] + " " + lineWords[2] + " " + lineWords[3]);
                }
            }

            x.close();
            pw.flush();
            pw.close();
            file.delete();
            File dump = new File(pathUser);
            newFile.renameTo(dump);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return ret;
    }

    public static boolean pushFile(DataInputStream in, String localPort, String userPath, DatagramSocket ds) throws IOException {

        String name = "";
        boolean state = true;
        long size = 0;

        try {
        name = in.readUTF();

        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream(getCwdServer(localPort, userPath) + "/" + name);

        size = in.readLong();
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;
        }

        } catch (IOException e) {
            e.printStackTrace();
            state = false;
        }

        FileInputStream f = new FileInputStream(getCwdServer(localPort, userPath) + "/" + name);

        byte b[]=new byte[1024];
        DatagramSocket dsoc=new DatagramSocket(2000);
        int i=0;
        while(f.available()!=0)
        {
            b[i]=(byte)f.read();
            i++;
        }
        f.close();
        dsoc.send(new DatagramPacket(b,i, InetAddress.getLocalHost(),6090));

        return state;

    }

    public static void pullFile(String path) {

        int bytes = 0;
        File file = new File(path);

        try {
            FileInputStream fileInputStream = new FileInputStream(file);

            out.writeUTF(file.getName());

            out.writeLong(file.length());

            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
                out.flush();

            }

            out.writeUTF("File downloaded");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String concatString(String aa) {

        String n = "";
        char [] ll = aa.toCharArray();
        int x = 0;

        for (int i = 0; i < ll.length; i++) {

            if (x==0) {
                if (String.valueOf(ll[ll.length - 1 - i]).equals("/")) {
                    x = ll.length - i;
                }
            }

        }

        for (int j = 0; j < x; j++) {
            n += ll[j];

        }

        return(n);
    }

    public static void clear(String path) {
        Boolean ret = false;
        File file = new File(path);
        File newFile = new File(concatString(path) + "tempFile.txt");

        try {
            FileWriter fw = new FileWriter(newFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            x = new Scanner(file);
            while (x.hasNext()) {
                String line = x.nextLine();
                String[] lineWords = line.split(" ", 4);
                pw.println(lineWords[0] + " " + lineWords[1] + " 0 " + lineWords[3]);
            }
            x.close();
            pw.flush();
            pw.close();
            file.delete();
            File dump = new File(path);
            newFile.renameTo(dump);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

}

}
