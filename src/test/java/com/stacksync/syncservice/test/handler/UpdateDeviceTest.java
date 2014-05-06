package com.stacksync.syncservice.test.handler;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;

public class UpdateDeviceTest {

	private static Handler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static User user1;
	private static User user2;

	@BeforeClass
	public static void initializeData() throws Exception {


			Config.loadProperties();

			String datasource = Config.getDatasource();
			ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

			handler = new SQLSyncHandler(pool);
			DAOFactory factory = new DAOFactory(datasource);

			Connection connection = pool.getConnection();

			workspaceDAO = factory.getWorkspaceDao(connection);
			userDao = factory.getUserDao(connection);

			user1 = new User(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

			userDao.add(user1);
			Workspace workspace1 = new Workspace(null, 1, user1, false, false);
			workspaceDAO.add(workspace1);

			user2 = new User(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

			userDao.add(user2);
			Workspace workspace2 = new Workspace(null, 1, user2, false, false);
			workspaceDAO.add(workspace2);


	}

	@AfterClass
	public static void cleanData() throws DAOException {
		// userDao.delete("aa");
	}

	@Test
	public void registerNewDevice() throws Exception {

		Device device = new Device();
		device.setUser(user1);
		device.setName("john's computer");
		device.setOs("Linux");
		device.setLastIp("15.26.156.98");
		device.setAppVersion("1.2.3");

		UUID result = handler.doUpdateDevice(device);

		System.out.println("Result: " + result + " | Device: " + device);

		assertNotEquals("-1", result);
	}

	@Test
	public void updateExistingDevice() throws Exception {

		Device device = new Device();
		device.setUser(user1);
		device.setName("john's computer");
		device.setOs("Linux");
		device.setLastIp("15.26.156.98");
		device.setAppVersion("1.2.3");

		UUID result1 = handler.doUpdateDevice(device);

		System.out.println("Result: " + result1 + " | Device: " + device);

		assertNotEquals("-1", result1);

		device.setLastIp("1.1.1.1");
		device.setAppVersion("3.3.3");

		UUID result2 = handler.doUpdateDevice(device);
		System.out.println("Result: " + result2 + " | Device: " + device);

		assertEquals(result1, result2);
	}

	@Test
	public void updateAlienDevice() throws Exception {

		Device device = new Device();
		device.setUser(user1);
		device.setName("john's computer");
		device.setOs("Linux");
		device.setLastIp("15.26.156.98");
		device.setAppVersion("1.2.3");

		UUID result = handler.doUpdateDevice(device);

		System.out.println("Result: " + result + " | Device: " + device);

		device.setUser(user2);
		device.setLastIp("1.1.1.1");
		device.setAppVersion("3.3.3");

		result = handler.doUpdateDevice(device);

		System.out.println("Result: " + result + " | Device: " + device);

		assertEquals("-1", result);
	}

}
