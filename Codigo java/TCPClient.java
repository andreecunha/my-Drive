package sockets;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.*;

import static java.lang.Thread.currentThread;
import static sockets.Connection.exists;
import static sockets.Connection.ls;
import static sockets.TCPServer.getInfo;

public class TCPClient {


	public static void main(String args[]) throws IOException, InterruptedException {

		if (args.length < 2) {
			System.out.println("java TCPClient hostname");
			System.exit(0);

		} else {

			int[] data = getInfo(args[1]);
			int port = data[0];
			int maxFailedRounds = data[1];
			int period = data[5];
			int port2 = data[7];

			connect(port, args, period, maxFailedRounds, port2);

		}
	}

	public static void connect(int serversocket, String[] args, int period, int maxFailedRounds, int port2) throws IOException, InterruptedException {

		try (Socket s = new Socket(args[0], serversocket)) {
			System.out.println("SOCKET=" + s);

			DataInputStream in = new DataInputStream(s.getInputStream());
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			Path cwd = Paths.get("").toAbsolutePath();
			String strCwd;
			String strFiles;
			String[] files;
			Scanner sc = new Scanner(System.in);

			try {

				while (true) {

					if (sc.hasNextLine()) {

						String texto = sc.nextLine();

						out.writeUTF(texto);

						String[] information = texto.split(" ", 2);

						if (information[0].equals("pullFile")) {

							String name = in.readUTF();

							int bytes = 0;
							FileOutputStream fileOutputStream = new FileOutputStream(cwd + "/" + name);

							long size = in.readLong();
							byte[] buffer = new byte[4 * 1024];
							while (size > 0 && (bytes = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
								fileOutputStream.write(buffer, 0, bytes);
								size -= bytes;
							}

						} else if (information[0].equals("pushFile")) {

							if (exists(information[1])) {

								int bytes = 0;
								File file = new File(information[1]);
								FileInputStream fileInputStream = new FileInputStream(file);

								out.writeUTF(file.getName());
								out.writeLong(file.length());

								byte[] buffer = new byte[4 * 1024];
								while ((bytes = fileInputStream.read(buffer)) != -1) {
									out.write(buffer, 0, bytes);
									out.flush();
								}

							}

						} else if (information[0].equals("clientLs")) {

							strFiles = null;
							strCwd = cwd.toString();
							files = ls(strCwd);

							for (int i = 0; i < files.length; i++)
								strFiles += files[i] + "\n";

							System.out.println(strFiles);

						} else if (information[0].equals("clientCd")) {

							if (exists(information[1])) {
								cwd = Paths.get(information[1]);
								System.out.println("Directoria atual: " + cwd.toString());

							} else
								System.out.println("Directory does not exist");

						}

						String data = in.readUTF();
						System.out.println("Received: " + data);

					}

				}

			} catch (IOException e) {

				System.out.println("Connecting to backup server");
				Thread.sleep(2 * period * maxFailedRounds);
				connect(port2, args, period, maxFailedRounds, 0);

				System.out.println("Sock:" + e.getMessage());

			}

		}

	}
}