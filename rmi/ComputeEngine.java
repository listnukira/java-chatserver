package rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

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
	
	public Object serverExecuteTask(Task task, String target) {
		try {
			Method executeMethod = task.getClass().getMethod("execute", new Class[] {});
			Gridify gridify = executeMethod.getAnnotation(Gridify.class);
			
			if (gridify == null) {
				return task.execute();
			} else {
				int clientNum = ChatServer.clientPool.size();
				Method mapper = task.getClass().getMethod(gridify.mapper(), new Class[] { int.class });
				Method reducer = task.getClass().getMethod(gridify.reducer(), new Class[] { Vector.class });
				
				@SuppressWarnings("unchecked")
				Vector<Task> map = (Vector<Task>) mapper.invoke(task, new Object[] {clientNum});
				return task.getClass().getSimpleName();
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return task.execute();
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
			return serverExecuteTask(task, target);
			//Method executeMethod = task.getClass().getMethod("execute", new Class[] {});
		}
	}
	
}
