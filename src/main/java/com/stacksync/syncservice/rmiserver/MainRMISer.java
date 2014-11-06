package com.stacksync.syncservice.rmiserver;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class MainRMISer {
	public static void main(String args[]) {
		try {
			LocateRegistry.createRegistry(1099);
			PostgresqlDeviceDAORMISer postgresqlDeviceDAO = new PostgresqlDeviceDAORMISer();
			PostgresqlItemDAORMISer postgresqlItemDAO = new PostgresqlItemDAORMISer();
			PostgresqlItemVersionDaoRMISer postgresqlItemVersionDao = new PostgresqlItemVersionDaoRMISer();
			PostgresqlUserDAORMISer postgresqlUserDAO = new PostgresqlUserDAORMISer();
			PostgresqlWorkspaceDAORMISer postgresqlWorkspaceDAO = new PostgresqlWorkspaceDAORMISer();
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
