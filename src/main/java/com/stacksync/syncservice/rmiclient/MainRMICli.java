package com.stacksync.syncservice.rmiclient;

import java.rmi.*;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.rmiserveri.*;

public class MainRMICli {
	public static void main(String args[]) {

		try {
			//DeviceDAORMISer addDeviceServer = (DeviceDAORMISer) Naming.lookup("rmi://0/DeviceServer");
			//ItemDAORMISer addItemServer = (ItemDAORMISer) Naming.lookup("rmi://0/ItemServer");
			//ItemVersionDAORMISer addItemVersionServer = (ItemVersionDAORMISer) Naming.lookup("rmi://0/ItemVersionServer");
			UserDAORMISer addUserServer = (UserDAORMISer) Naming.lookup("rmi://0/UserServer");
			//WorkspaceDAORMISer addWorkspaceServer = (WorkspaceDAORMISer) Naming.lookup("rmi://0/WorkspaceServer");

			UserRMI user = new UserRMI();
			user.setName("Laura");
			user.setId(UUID.randomUUID());
			user.setEmail("laura@jo.com");
			user.setQuotaLimit(2048);
			user.setQuotaUsed(1403);

			addUserServer.add(user);

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}
}