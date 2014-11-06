package com.stacksync.syncservice.rmiclient;

import java.rmi.*;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.rmiserveri.*;

public class RmiClientMain {
	public static void main(String args[]) {
		
		try {
			//String addServerURL = "rmi://" + args[0] + "/AddServer";
			DeviceDAO addDeviceServer = (DeviceDAO) Naming.lookup("rmi://DeviceServer");
			ItemDAO addItemServer = (ItemDAO) Naming.lookup("rmi://ItemServer");
			ItemVersionDAO addItemVersionServer = (ItemVersionDAO) Naming.lookup("rmi://ItemVersionServer");
			UserDAO addUserServer = (UserDAO) Naming.lookup("rmi://UserServer");
			WorkspaceDAO addWorkspaceServer = (WorkspaceDAO) Naming.lookup("rmi://WorkspaceServer");
			
			User user = new User();
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