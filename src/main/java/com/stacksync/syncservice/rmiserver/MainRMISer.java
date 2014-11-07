package com.stacksync.syncservice.rmiserver;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

import com.stacksync.syncservice.rmiserveri.WorkspaceDAORMIIfc;

public class MainRMISer {
	public static void main(String args[]) {
		try {
			LocateRegistry.createRegistry(1099);
			
			DeviceDAORMISer postgresqlDeviceDAO = new DeviceDAORMISer();
			ItemDAORMISer postgresqlItemDAO = new ItemDAORMISer();
			ItemVersionDAORMISer postgresqlItemVersionDao = new ItemVersionDAORMISer();
			UserDAORMISer postgresqlUserDAO = new UserDAORMISer();
			WorkspaceDAORMISer postgresqlWorkspaceDAO = new WorkspaceDAORMISer();
			
			Naming.rebind("DeviceServer", postgresqlDeviceDAO);
			Naming.rebind("ItemServer", postgresqlItemDAO);
			Naming.rebind("ItemVersionServer", postgresqlItemVersionDao);
			Naming.rebind("UserServer", postgresqlUserDAO);
			Naming.rebind("WorkspaceServer", postgresqlWorkspaceDAO);
			
			System.out.println("SERVER CONNECTED");
			
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}
}
