package ChatServer;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.net.*;
import java.io.*;

import rmi.*;

public class ChatServer {
	static int port;
	int connCount = 0;
	public static Vector<clientThread> clientPool = new Vector<clientThread>(20);

	public void runServer() {
		try {
			ServerSocket SS = new ServerSocket(port);
			FileWriter connLog = new FileWriter("connect_log.txt", false);

			System.out.println("ChatServer is created and waiting Client to connect...");
			connLog.write("IP/port\tlogID\n");
			connLog.flush();

			/* Wait Client connection */
			while (true) {
				Socket socket = SS.accept();
				connCount++;
				connLog.write(socket.getInetAddress().getHostAddress() + "/" + socket.getPort() + "\t" + connCount + "\n");
				connLog.flush();
				
				clientThread t = new clientThread(socket, clientPool);
				clientPool.add(t);
				t.start();
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	static void usage() {
		System.out.println("Usage: java ChatServer [port]");
		System.exit(1);
	}

	public static void main(String args[]) {
		if (args.length != 1) {
			usage();
		}
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		
		try {
			port = Integer.parseInt(args[0]);
			
			/* rmi server */
			Compute computeEngine = (Compute) new ComputeEngine();
			Registry registry = LocateRegistry.createRegistry(1099);
			registry.rebind("@SERVER", computeEngine);
			/* chat server */
			ChatServer Server = new ChatServer();
			Server.runServer();
		} catch (IllegalArgumentException e) {
			usage();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}