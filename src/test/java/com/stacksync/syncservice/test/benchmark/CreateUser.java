package com.stacksync.syncservice.test.benchmark;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.test.benchmark.db.DatabaseHelper;

public class CreateUser {

	
	public static void main(String[] args) throws Exception{
		
		DatabaseHelper dbHelper = new DatabaseHelper();
		
		dbHelper.deleteUser("AUTH_e26e8353dbd043ae857ad6962e02f5cc");
		
		User user = new User(null, "user1", "AUTH_e26e8353dbd043ae857ad6962e02f5cc", "email@email.com", 1000, 100);
		dbHelper.addUser(user);
		
		Workspace workspace = new Workspace(null, 1, user);
		dbHelper.addWorkspace(user, workspace);

		String deviceName = "tester1:tester1" + "_device";
		Device device = new Device(null, deviceName, user);
		dbHelper.addDevice(device);

	}
}
