package ChatServer;

import java.util.*;
import java.net.*;
import java.io.*;

public class ChatServer {
	static int port;
	int connCount = 0;
	static Vector<clientThread> clientPool = new Vector<clientThread>(20);

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
		}
		catch (IOException e) {
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
		try {
			port = Integer.parseInt(args[0]);
			ChatServer Server = new ChatServer();
			Server.runServer();
		} catch (IllegalArgumentException e) {
			usage();
		}
	}
}

class clientThread extends java.lang.Thread {

	static Vector<Msg> msgPool = new Vector<Msg>();
	static int msgid = 0;
	BufferedReader sin = null;
	PrintWriter sout = null;
	Socket socket = null;
	Vector<clientThread> clientPool;
	String name = "(Unknown)";
	String host;
	PrintWriter login = null;
	PrintWriter logout = null;
	int port;

	public clientThread(Socket socket, Vector<clientThread> clientPool) {
		this.socket = socket;
		this.host = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		this.clientPool = clientPool;
	}

	public synchronized int getMsgid() {
		msgid++;
		return msgid;
	}

	boolean getUsername() throws IOException {
		String tempname;
		try {
			if (sin.ready() && ((tempname = sin.readLine()) != null)) {
				if (tempname.length() == 0) {
					sout.println("/msg Error: No username is input.");
					sout.println("/msg Username: ");
				} else if (isSameName(tempname) == true) {
					sout.println("/msg Error: The user '" + tempname + "' is already online. Please change a name.");
					sout.println("/msg Username: ");
				} else {
					name = tempname;
					return true;
				}
			}
		} catch (IOException e) {
			System.err.println("IOException in getUsername(): " + e);
			throw new IOException();
		}

		return false;
	}

	boolean isSameName(String tempname) {
		synchronized (clientPool) {
			for (clientThread t : clientPool) {
				if (t.name.equals(tempname))
					return true;
			}
		}
		return false;
	}

	void prtLoginMsg() {
		sout.println("/msg ***********************************************");
		sout.println("/msg ** " + name + ", welcome to the chat system.");
		sout.println("/msg ***********************************************");
	}

	void bocast(String msg) {
		synchronized (clientPool) {
			for (clientThread t : clientPool) {
				if (t.name.equals("(Unknown)") == false) {
					t.sout.println(msg);
					t.logout.println(msg);
				}
			}
		}
	}

	void prtOnlineUsr() {
		sout.println("/msg Name\tIP/port");
		//logout.println("/msg Name\tIP/port");
		synchronized (clientPool) {
			for (clientThread t : clientPool) {
				if (t.name.equals(name)) {
					sout.println("/msg " + t.name + "\t" + t.host + "/" + t.port + "\t <--myself");
					logout.println("/msg " + t.name + "\t" + t.host + "/" + t.port + "\t <--myself");
				} else {
					sout.println("/msg " + t.name + "\t" + t.host + "/" + t.port);
					logout.println("/msg " + t.name + "\t" + t.host + "/" + t.port);
				}
			}
		}
	}

	void prtYellMsg(String msg) {
		String[] splitMsg = msg.split(" ", 2);
		String something;
		if (splitMsg.length == 1)
			something = "";
		else
			something = splitMsg[1];

		bocast("/msg " + name + " yelled: " + something);
	}

	void prtTellMsg(String msg) {
		String[] splitMsg = msg.split(" ", 3);
		String something;
		
		/* if /tell no given target, return */
		if (splitMsg.length == 1) {
			sout.println("/msg Error: No target was given.");
			logout.println("/msg Error: No target was given.");
			return;
		} else if (splitMsg.length == 2) {
			something = "";
		} else {
			something = splitMsg[2];
		}

		synchronized (clientPool) {
			for (clientThread t : clientPool) {
				if (t.name.equals(splitMsg[1])) {
					t.sout.println("/msg " + name + " told " + t.name + ": " + something);
					t.logout.println("/msg " + name + " told " + t.name + ": " + something);
					return;
				}
			}
		}

		/* no user find */
		sout.println("/msg Error: '" + splitMsg[1] + "' is not online.");
		logout.println("/msg Error: '" + splitMsg[1] + "' is not online.");
	}

	/*	msg ex: /post String str..
	 * 			/post RectangleWidget 50 50 #0000ff 50 12
	 * 			/post CircleWidget 10 10
	 */
	void prtPostMsg(String msg) {
		String[] splitMsg = msg.split(" ", 3);

		/* argument check */
		if (splitMsg.length == 1) {
			sout.println("/msg Error: No post type.");
			logout.println("/msg Error: No post type.");
			return;
		}

		if (splitMsg[1].equals("String") == false && splitMsg[1].equals("RectangleWidget") == false &&
			splitMsg[1].equals("JugglerWidget") == false && splitMsg[1].equals("CircleWidget") == false) {
			sout.println("/msg Error: No such post type.");
			logout.println("/msg Error: No such post type.");
			return;
		}
		// End argument check

		Msg m = new Msg(name, msg, getMsgid());
		if (m.argsIsOK() == false) {
			sout.println("/msg Error: Wrong arguments.");
			logout.println("/msg Error: Wrong arguments.");
			return;
		}
		
		bocast("/post " + m.getContext());

		synchronized (msgPool) {
			msgPool.add(m);
		}
	}

	void rmPostMsg(String msg) {
		String[] splitMsg = msg.split(" ");

		/* if only one argument, return error message */
		if (splitMsg.length == 1) {
			sout.println("/msg Error: No msg id.");
			logout.println("/msg Error: No msg id.");
			return;
		}

		synchronized (msgPool) {
			for (Msg m : msgPool) {
				if (Integer.parseInt(splitMsg[1]) == m.getMsgid()) {
					if (name.equals(m.getUsr())) {
						bocast("/remove " + name + " " + m.getMsgid());
						msgPool.remove(m);
					} else {
						sout.println("/msg Permission denied.");
						logout.println("/msg Permission denied.");
					}
					return;
				}
			}
		}

		/* if no msdid match, return error message */
		sout.println("/msg Error: No such msg id.");
		logout.println("/msg Error: No such msg id.");

	}

	/* msg ex: /move 1 20 30 */
	void moveObj(String msg) {
		String[] splitMsg = msg.split(" ", 4);
		
		/* parse argument */
		try {
			if (splitMsg.length != 4 || Integer.parseInt(splitMsg[2]) < 0 || Integer.parseInt(splitMsg[3]) < 0) {
				sout.println("/msg Error: Wrong arguments.");
				logout.println("/msg Error: Wrong arguments.");
				return;
			}
		} catch (IllegalArgumentException e) {
			sout.println("/msg Error: Wrong arguments.");
			logout.println("/msg Error: Wrong arguments.");
			return;
		}
		
		/* midify server side Msg */
		synchronized (msgPool) {
			for (Msg m : msgPool) {
				if (m.getMsgid() == Integer.parseInt(splitMsg[1]) && m.getUsr().equals(name)) {
					String[] splited = m.getMsg().split(" ", 3);
					if (splited.length == 2) {
						m.setMsg(String.format("%s %s", splitMsg[2], splitMsg[3]));
					} else {
						m.setMsg(String.format("%s %s %s", splitMsg[2], splitMsg[3], splited[2]));
					}
					
					/* motify all client */
					bocast(msg);
					return;
				}
			}
		}
		sout.println("/msg Permission denied or Object not found.");
		logout.println("/msg Permission denied or Object not found.");
	}
	
	void sendPostLog() {
		synchronized (msgPool) {
			for (Msg m : msgPool) {
				sout.println("/post " + m.getContext());
				logout.println("/post " + m.getContext());
			}
		}
	}

	void kickClient(String kickedName) {
		bocast("/kick " + kickedName);
	}

	public void run() {
		String cmd;
		try {
			sout = new PrintWriter(socket.getOutputStream(), true);
			sin = new BufferedReader( new InputStreamReader(socket.getInputStream()) );

			/* get username first */
			while (getUsername() == false);

			/*  if username vaild
			 *  print welcome message and open log file
			 */
			login = new PrintWriter( (new FileWriter("input_" + name + ".txt", true)), true);
			logout = new PrintWriter( (new FileWriter("output_" + name + ".txt", true)), true);
			bocast("/msg " + name + " is connecting to the chat server.");
			prtLoginMsg();
			sendPostLog();

			/* receive client's command */
			while ((cmd = sin.readLine()) != null) {
				login.println(cmd);
				cmd = cmd.trim();
				String[] splitCmd = cmd.split(" ", 2);
				if (splitCmd[0].equals("/leave")) {
					break;
				} else if (splitCmd[0].equals("/who")) {
					prtOnlineUsr();
				} else if (splitCmd[0].equals("/yell")) {
					prtYellMsg(cmd);
				} else if (splitCmd[0].equals("/kick")) {
					kickClient(splitCmd[1]);
				} else if (splitCmd[0].equals("/tell")) {
					prtTellMsg(cmd);
				} else if (splitCmd[0].equals("/post")) {
					prtPostMsg(cmd);
				} else if (splitCmd[0].equals("/remove")) {
					rmPostMsg(cmd);
				} else if (splitCmd[0].equals("/move")) {
					moveObj(cmd);
				} else {
					sout.println("/msg *** Your message command '" + splitCmd[0] + "' is incorrect. ***");
					logout.println("/msg **** Your message command '" + splitCmd[0] + "' is incorrect.");
				}
			}
		} catch (IOException e) {
			
		} finally {
			/* client thread leave */
			synchronized (clientPool) {
				for (clientThread t : clientPool) {
					if (t == this) {
						System.out.println(t.name + " leave.");
						clientPool.remove(t);
						break;
					}
				}
			}

			try {
				bocast("/msg " + name + " is leaving the chat server.");
				sin.close();
				sout.close();
				socket.close();
			} catch (IOException e) {
			}
		}
	}
}
