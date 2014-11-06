package com.stacksync.syncservice.db;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.sql.Connection;

import com.stacksync.syncservice.db.postgresql.PostgresqlDeviceDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlItemDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlItemVersionDao;
import com.stacksync.syncservice.db.postgresql.PostgresqlUserDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlWorkspaceDAO;

public class DAOFactory {

	private String type;

	public DAOFactory(String type) {
		this.type = type;
	}

	public WorkspaceDAO getWorkspaceDao() {
		return new PostgresqlWorkspaceDAO();
	}

	public UserDAO getUserDao() throws RemoteException {
		try {
//			LocateRegistry.createRegistry(1099);
//			PostgresqlUserDAO addServerImpl = new PostgresqlUserDAO();
//			// System.out.println("sdfsdfsdf");
//			Naming.rebind("AddServer", addServerImpl);
			return (UserDAO) Naming.lookup("rmi://AddServer");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public ItemDAO getItemDAO() {
		return new PostgresqlItemDAO();
	}

	public ItemVersionDAO getItemVersionDAO() {
		return new PostgresqlItemVersionDao();
	}

	public DeviceDAO getDeviceDAO() {
		return new PostgresqlDeviceDAO();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
