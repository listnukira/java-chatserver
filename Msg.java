package ChatServer;

import java.io.IOException;

public class Msg {

	private String type;
	private String usr;
	private String msg;
	private int msgid;


	/*	msg : /post String This is a test.
	 *	msg : /post RectangleWidget 10 20 #8b00ff 60 80
	 *	msg : /post RectangleWidget 10 20
	 */
	public Msg(String usr, String msg, int msgid) {
		String[] splitMsg = msg.split(" ", 3);

		if (splitMsg.length == 2) {
			this.msg = "";
		} else {
			this.msg = splitMsg[2];
		}

		this.type = splitMsg[1];
		this.usr = usr;
		this.msgid = msgid;
	}

	public boolean argsIsOK() {
		if (type.equals("String")) {
			return true;
		} else {
			try {
				String[] splitXY = msg.split(" ");
				if (Integer.parseInt(splitXY[0]) >=0 && Integer.parseInt(splitXY[1]) >=0)
					return true;
			} catch (IllegalArgumentException e){
				return false;
			}
		}
		return false;
	}
	
	public String getContext() {
		return usr + " " + msgid + " " + type + " " + msg;
	}

	public int getMsgid() {
		return msgid;
	}

	public String getUsr() {
		return usr;
	}

	public String getMsg() {
		return msg;
	}

	public String getType() {
		return type;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
}
