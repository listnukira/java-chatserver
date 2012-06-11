package rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import task.*;
import ChatServer.*;

public class ComputeEngine extends UnicastRemoteObject implements Compute {
	
	private static final long serialVersionUID = 1L;
	int mode; // 0: server mode, 1: client mode
	
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
			
			if (gridify != null && target == null) {
				int clientNum = ChatServer.clientPool.size();
				Method mapper = task.getClass().getMethod(gridify.mapper(), new Class[] { int.class });
				Method reducer = task.getClass().getMethod(gridify.reducer(), new Class[] { Vector.class });
				
				@SuppressWarnings("unchecked")
				Vector<Task> map = (Vector<Task>) mapper.invoke(task, new Object[] {clientNum});
				Vector<Object> result = new Vector<Object>(clientNum);
				CountDownLatch threadSignal = new CountDownLatch(clientNum);			
				
				synchronized (ChatServer.clientPool) {
					for (int i = 0; i < clientNum; ++i) {
						Thread t = new smallTask(map.get(i), ChatServer.clientPool.get(i).name, 
								result, threadSignal);
						t.start();
					}
				}
				threadSignal.await();
				return (Object) reducer.invoke(task, result);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return task.execute();
	}
	
	@Override
	public Object executeTask(Task task, String target) {
		mode = 0;
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
		}
	}
	
	public class smallTask extends Thread {
		private CountDownLatch threadsSignal;
		private String name;
		private Task task;
		private Vector<Object> result;
		
		public smallTask(Task task, String name, Vector<Object> result, CountDownLatch threadsSignal) {
			this.task = task;
			this.name = name;
			this.result = result;
			this.threadsSignal = threadsSignal;
		}
		
		@Override
		public void run() {
			try {
				Compute clientCompute = (Compute) Naming.lookup("rmi://localhost:1099/" + name);
				Object object = clientCompute.executeTask(task, name);
				result.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
			threadsSignal.countDown();
		}
	}
}
