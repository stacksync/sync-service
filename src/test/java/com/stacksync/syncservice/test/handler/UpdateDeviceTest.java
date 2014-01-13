package com.stacksync.syncservice.test.handler;

import java.sql.Connection;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.util.Config;

public class UpdateDeviceTest {

	private static Handler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static User user;

	@BeforeClass
	public static void initializeData() {

		try {
			Config.loadProperties();

			String datasource = Config.getDatasource();
			ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

			handler = new SQLHandler(pool);
			DAOFactory factory = new DAOFactory(datasource);

			Connection connection = pool.getConnection();

			workspaceDAO = factory.getWorkspaceDao(connection);
			userDao = factory.getUserDao(connection);

			user = new User(null, "junituser", "aa", "aa", 1000, 100);
			try{
			userDao.add(user);
				Workspace workspace1 = new Workspace(null, "junituser1/", 1,
						user);
				workspaceDAO.add(workspace1);
			}catch(DAOException e)
			{
				System.out.println("User already exists.");
				user = userDao.findByCloudId("aa");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void cleanData() throws DAOException {
		//userDao.delete("aa");
	}

	@Test
	public void registerNewDevice() throws DAOException {

		
		Device device = new Device();
		device.setUser(user);
		device.setName("john's computer");
		device.setOs("Linux");
		device.setLastIp("15.26.156.98");
		device.setAppVersion("1.2.3");
		
		Long result = handler.doUpdateDevice(device);

		System.out.println("Result: " + result + " | Device: " + device);

		assertNotEquals(-1L, result.longValue());
	}
	
	@Test
	public void updateExistingDevice() throws DAOException {

		
		Device device = new Device();
		device.setUser(user);
		device.setName("john's computer");
		device.setOs("Linux");
		device.setLastIp("15.26.156.98");
		device.setAppVersion("1.2.3");
		
		Long result1 = handler.doUpdateDevice(device);

		System.out.println("Result: " + result1 + " | Device: " + device);
		
		user.setId(999L);
		device.setLastIp("1.1.1.1");
		device.setAppVersion("3.3.3");
		
		Long result2 = handler.doUpdateDevice(device);
		System.out.println("Result: " + result2 + " | Device: " + device);
		
		assertEquals(result1.longValue(), result2.longValue());
	}
	
	@Test
	public void updateAlienDevice() throws DAOException {

		
		Device device = new Device();
		device.setUser(user);
		device.setName("john's computer");
		device.setOs("Linux");
		device.setLastIp("15.26.156.98");
		device.setAppVersion("1.2.3");
		
		Long result= handler.doUpdateDevice(device);

		System.out.println("Result: " + result + " | Device: " + device);
		
		user.setId(999999999L);
		device.setLastIp("1.1.1.1");
		device.setAppVersion("3.3.3");
		
		result = handler.doUpdateDevice(device);
		
		System.out.println("Result: " + result + " | Device: " + device);
		
		assertEquals(-1L, result.longValue());
	}
	

}
