package ChatServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import task.*;

public class ComputeEngine extends UnicastRemoteObject implements Compute {
	
	public ComputeEngine() throws RemoteException {}
	
	public Object executeTask(Task t, String target) {
		return t;
	}
}
