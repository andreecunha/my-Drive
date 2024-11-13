package sockets;

import java.io.*;
import java.net.*;


class Heartbeat implements Runnable {

    private static String mode;
    private static int maxfailedrounds;
    private static int timeout;
    private static int bufsize;
    private static int port;
    private static int period;
    private static int port2;
    private static int downloadPort;
    private static int UDPdownload;

    public Heartbeat(String mode, int maxfailedrounds, int timeout, int bufsize, int port, int period, int port2, int downloadPort, int UDPdownload) {

        this.mode = mode;
        this.maxfailedrounds = maxfailedrounds;
        this.timeout = timeout;
        this.bufsize = bufsize;
        this.port = port;
        this.period = period;
        this.port2 = port2;
        this.downloadPort = downloadPort;
        this.UDPdownload = UDPdownload;

    }

    public static void recFile() throws IOException {

        byte b[]=new byte[3072];
        DatagramSocket dsoc=new DatagramSocket(6090);
        FileOutputStream f=new FileOutputStream("/home/rodrigo/Desktop/SD_Projeto/home2/users/user/ta.txt");
        {
            DatagramPacket dp=new DatagramPacket(b,b.length);
            dsoc.receive(dp);
            f.write(b, 0, b.length);

        }

    }


    @Override
    public void run() {

        if (mode.equals("primary")) {

            try (DatagramSocket ds = new DatagramSocket(port)) {

                while (true) {
                    byte buf[] = new byte[bufsize];
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    ds.receive(dp);
                    ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                    DataInputStream dis = new DataInputStream(bais);
                    int count = dis.readInt();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(count);
                    byte resp[] = baos.toByteArray();
                    DatagramPacket dpresp = new DatagramPacket(resp, resp.length, dp.getAddress(), dp.getPort());
                    ds.send(dpresp);
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }


        } else if (mode.equals("secondary")) {

            int count = 1;

            InetAddress ia = null;
            try {
                ia = InetAddress.getByName("localhost");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            try {

                DatagramSocket ds = new DatagramSocket(port2);
                ds.setSoTimeout(timeout);


                int failedheartbeats = 0;
                while (failedheartbeats < maxfailedrounds) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeInt(count++);
                        byte[] buf = baos.toByteArray();

                        DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, port);
                        ds.send(dp);

                        byte[] rbuf = new byte[bufsize];
                        DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);

                        ds.receive(dr);
                        failedheartbeats = 0;
                        ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
                        DataInputStream dis = new DataInputStream(bais);
                        int n = dis.readInt();
                        System.out.println("Got: " + n + ".");
                        failedheartbeats = 0;
                    } catch (SocketTimeoutException ste) {
                        failedheartbeats++;
                        System.out.println("Failed heartbeats: " + failedheartbeats);
                    }

                    Thread.sleep(period);

                }

                int numero = 0;

                try (ServerSocket listenSocket = new ServerSocket(downloadPort)) {

                    System.out.println("A escuta no porto " + downloadPort);
                    System.out.println("LISTEN SOCKET=" + listenSocket);


                    while (true) {

                        Socket clientSocket = listenSocket.accept();
                        System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                        numero++;

                        new Connection(clientSocket, numero, "/home/rodrigo/Desktop/SD_Projeto/User_Pass.txt", downloadPort);

                    }

                } catch (IOException e) {
                    System.out.println("Listen:" + e.getMessage());
                }


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        } else if (mode.equals("files")) {


            try {
                recFile();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


        }
    }


