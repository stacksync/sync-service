package com.stacksync.syncservice.rmiserver;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class PostgresqlDAOmain {
	public static void main(String args[]) {
		try {
			LocateRegistry.createRegistry(1099);
			PostgresqlDeviceDAO postgresqlDeviceDAO = new PostgresqlDeviceDAO();
			PostgresqlItemDAO postgresqlItemDAO = new PostgresqlItemDAO();
			PostgresqlItemVersionDao postgresqlItemVersionDao = new PostgresqlItemVersionDao();
			PostgresqlUserDAO postgresqlUserDAO = new PostgresqlUserDAO();
			PostgresqlWorkspaceDAO postgresqlWorkspaceDAO = new PostgresqlWorkspaceDAO();
			System.out.println("sdfsdfsdf-1");
			Naming.rebind("DeviceServer", postgresqlDeviceDAO);
			Naming.rebind("ItemServer", postgresqlItemDAO);
			Naming.rebind("ItemVersionServer", postgresqlItemVersionDao);
			Naming.rebind("UserServer", postgresqlUserDAO);
			Naming.rebind("WorkSpaceServer", postgresqlWorkspaceDAO);
			System.out.println("sdfsdfsdf-2");
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}
}
