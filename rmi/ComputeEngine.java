package rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import task.*;
import ChatServer.*;

public class ComputeEngine extends UnicastRemoteObject implements Compute {
	
	int mode = 0; // 0: server mode, 1: client mode
	
	public ComputeEngine() throws RemoteException {
		super();
	}
	
	public Object clientExecuteTask(Task task, String target) {
		try {
			Compute clientCompute = (Compute) Naming.lookup("rmi://localhost:1099/" + target);
			return clientCompute.executeTask(task, target);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Object executeTask(Task task, String target) {
		
		synchronized (ChatServer.clientPool) {
			for (clientThread client : ChatServer.clientPool) {
				if (client.name.equals(target)) {
					mode = 1;
					break;
				}
			}
		}
		
		if (mode == 1) {
			return clientExecuteTask(task, target);
		} else {
			return task.execute();
		}
		//return t.execute();
	}
	
}
