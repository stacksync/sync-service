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

	public WorkspaceDAO getWorkspaceDao(Connection connection) {
		return new PostgresqlWorkspaceDAO(connection);
	}

	public UserDAO getUserDao(Connection connection) {
		return new PostgresqlUserDAO(connection);
	}

	public ItemDAO getItemDAO(Connection connection) {
		return new PostgresqlItemDAO(connection);
	}

	public ItemVersionDAO getItemVersionDAO(Connection connection) {
		return new PostgresqlItemVersionDao(connection);
	}

	public DeviceDAO getDeviceDAO(Connection connection) {
		return new PostgresqlDeviceDAO(connection);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
