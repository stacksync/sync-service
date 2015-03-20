/**
 * 
 */
package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class PostgresqlDeviceDao {
	private static Connection connection;
	private static User user;
	private static UserDAO userDao;
	private static Device device;
	private static DeviceDAO deviceDao;
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

		deviceDao = factory.getDeviceDAO(connection);
	}

	@AfterClass
	public static void testEnd() throws DAOException, SQLException {
		userDao.delete(user.getId());
		connection.close();
	}

	@Before
	public void createDevice() throws DAOException {
		device = new Device();
		device.setAppVersion("1");
		device.setLastIp("192.168.1.1");
		device.setName("1+1");
		device.setOs("Android");
		device.setUser(user);

		deviceDao.add(device);
	}

	@After
	public void deleteDevice() throws DAOException {
		deviceDao.delete(user.getId(), device.getId());
	}

	@Test
	public void getDevice() throws DAOException {
		UUID userID = user.getId();
		UUID deviceID = device.getId();

		Device d = deviceDao.get(userID, deviceID);

		assertEquals(deviceID, d.getId());
	}

	@Test
	public void updateDevice() throws DAOException {
		String appVersion = "asdf";
		UUID userID = user.getId();

		device.setAppVersion(appVersion);
		deviceDao.update(device);

		Device d = deviceDao.get(userID, device.getId());

		assertEquals(appVersion, d.getAppVersion());
	}

	public static String nextString() {
		return new BigInteger(130, random).toString(32);
	}

}
