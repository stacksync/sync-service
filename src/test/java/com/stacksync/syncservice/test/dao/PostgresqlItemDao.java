/**
 * 
 */
package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class PostgresqlItemDao {
	private static Connection connection;
	private static User user;
	private static UserDAO userDao;
	private static Workspace workspace;
	private static WorkspaceDAO workspaceDao;
	private static Device device;
	private static DeviceDAO deviceDao;
	private static Item item;
	private static ItemDAO itemDao;
	private static SecureRandom random;

	@BeforeClass
	public static void testSetup() throws ClassNotFoundException, SQLException, DAOException {
		random = new SecureRandom();

		String HOST = "10.30.233.113";
		int PORT = 5432;
		String DB = "testdb";
		String USER = "testos";
		String PASS = "testos";

		Class.forName("org.postgresql.Driver");
		connection = DriverManager.getConnection("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB, USER, PASS);

		String dataSource = "postgresql";
		DAOFactory factory = new DAOFactory(dataSource);

		userDao = factory.getUserDao(connection);
		workspaceDao = factory.getWorkspaceDao(connection);
		deviceDao = factory.getDeviceDAO(connection);

		// Create a user for the workspace
		user = new User();
		user.setName(nextString());
		user.setId(UUID.randomUUID());
		user.setEmail(nextString());
		user.setSwiftUser(nextString());
		user.setSwiftAccount(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		workspace = new Workspace();
		workspace.setLatestRevision(0);
		workspace.setOwner(user);
		workspaceDao.add(workspace);

		device = new Device();
		device.setAppVersion("1");
		device.setLastIp("192.168.1.1");
		device.setName("1+1");
		device.setOs("Android");
		device.setUser(user);

		deviceDao.add(device);

		itemDao = factory.getItemDAO(connection);
	}

	@AfterClass
	public static void testEnd() throws DAOException, SQLException {
		userDao.delete(user.getId()); // This should remove everything else...
		connection.close();
	}

	@Test
	public void test() throws DAOException {
		UUID userID = user.getId();

		item = new Item();
		item.setWorkspace(workspace);
		item.setLatestVersion(1L);
		item.setParent(null);
		item.setFilename(nextString());
		item.setMimetype("image/jpeg");
		item.setIsFolder(false);
		item.setClientParentFileVersion(1L);

		itemDao.add(user, item);

		Item i = itemDao.findById(userID, item.getId());
		assertEquals(item.getId(), i.getId());

		String name = "asdf";
		item.setFilename(name);
		itemDao.update(user, item);
		i = itemDao.findById(userID, item.getId());

		assertEquals(name, i.getFilename());

		// Since there are no itemVersions in this test, getItems by workspace
		// id will return an empty list.

		try {
			itemDao.getItemsByWorkspaceId(userID, workspace.getId());
		} catch (DAOException e) {
			assertTrue(false);
		}

	}

	public static String nextString() {
		return new BigInteger(130, random).toString(32);
	}

}
