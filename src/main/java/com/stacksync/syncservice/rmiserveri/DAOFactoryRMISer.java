package com.stacksync.syncservice.rmiserveri;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.sql.Connection;

import com.stacksync.syncservice.rmiserver.*;

public class DAOFactoryRMISer {

	private String type;

	public DAOFactoryRMISer(String type) {
		this.type = type;
	}

	public WorkspaceDAORMISer getWorkspaceDao() throws RemoteException {
		return new PostgresqlWorkspaceDAORMISer();
	}

	public UserDAORMISer getUserDao() throws RemoteException {
		return new PostgresqlUserDAORMISer();
	}

	public ItemDAORMISer getItemDAO() throws RemoteException {
		return new PostgresqlItemDAORMISer();
	}

	public ItemVersionDAORMISer getItemVersionDAO() throws RemoteException {
		return new PostgresqlItemVersionDaoRMISer();
	}

	public DeviceDAORMISer getDeviceDAO() throws RemoteException {
		return new PostgresqlDeviceDAORMISer();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
