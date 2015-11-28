package com.stacksync.syncservice.test.handler;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.UUID;

public class UpdateDeviceTest {

   private static Handler handler;
   private static WorkspaceDAO workspaceDAO;
   private static UserDAO userDao;
   private static UserRMI user1;
   private static UserRMI user2;

   @BeforeClass
   public static void initializeData() throws Exception {

      Config.loadProperties();

      String datasource = Config.getDatasource();
      ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

      handler = new SQLSyncHandler(pool);
      DAOFactory factory = new DAOFactory(datasource);

      Connection connection = pool.getConnection();
		connection.cleanup();

      workspaceDAO = factory.getWorkspaceDao(connection);
      userDao = factory.getUserDao(connection);

      user1 = new UserRMI(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

      userDao.add(user1);
      WorkspaceRMI workspace1 = new WorkspaceRMI(null, 1, user1, false, false);
      workspaceDAO.add(workspace1);

      user2 = new UserRMI(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

      userDao.add(user2);
      WorkspaceRMI workspace2 = new WorkspaceRMI(null, 1, user2, false, false);
      workspaceDAO.add(workspace2);


   }

   @AfterClass
   public static void cleanData() throws DAOException {
      // userDao.delete("aa");
   }

	/*@Test
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
	}*/

}
