package com.stacksync.syncservice.test.rmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.xml.DOMConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.rmiserver.*;
import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.util.Config;

public class RMITest {

	private static Connection connection;
	private static WorkspaceDAORMIIfc workspaceDAO;
	private static UserDAORMIIfc userDao;
	private static ItemDAORMIIfc objectDao;
	private static ItemVersionDAORMIIfc oversionDao;
	private static SecureRandom random = new SecureRandom();

	@BeforeClass
	public static void testSetup() throws IOException, SQLException,
			DAOConfigurationException {

		Config.loadProperties();

		String dataSource = "postgresql";
		/*DAOFactoryRMIIfc factory = new DAOFactoryRMIIfc(dataSource);
		connection = ConnectionPoolFactoryRMISer.getConnectionPool(dataSource)
				.getConnection();
		workspaceDAO = factory.getWorkspaceDao();
		if (factory.getUserDao() != null) {
			userDao = factory.getUserDao();
		} else
			System.out.println("UserDAO --> NULL");
		objectDao = factory.getItemDAO();
		oversionDao = factory.getItemVersionDAO();*/

		try {
			LocateRegistry.createRegistry(1099);
			UserDAORMISer addPostgresqlUserDAO = new UserDAORMISer();
			System.out.println("sdfsdfsdf");
			Naming.rebind("AddServer", addPostgresqlUserDAO);
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	protected String nextString() {
		return new BigInteger(130, random).toString(32);
	}

	@Test
	public void testCreateNewValidUser() throws IllegalArgumentException,
			DAOException, RemoteException {
		UserRMI user = new UserRMI();
		user.setName(nextString());
		user.setId(UUID.randomUUID());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		System.out.println("testCreateNewValidUser");

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {
			assertTrue(true);
		}

	}

	@Test
	public void testCreateNewUserSameId() throws IllegalArgumentException,
			DAOException, RemoteException {

		UUID userId = UUID.randomUUID();

		UserRMI user = new UserRMI();
		user.setName(nextString());
		user.setId(userId);
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {
			UserRMI user2 = new UserRMI();
			user2.setName(nextString());
			user2.setId(userId);
			user2.setEmail(nextString());
			user2.setQuotaLimit(2048);
			user2.setQuotaUsed(1403);

			userDao.add(user2);
			assertTrue("User should not have been created", false);
		}
	}

	@Test
	public void testUpdateExistingUserOk() throws IllegalArgumentException,
			DAOException, RemoteException {

		UserRMI user = new UserRMI();
		user.setName(nextString());
		user.setId(UUID.randomUUID());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {

			UUID id = user.getId();
			String newName = nextString();
			UUID newUserId = UUID.randomUUID();
			String newEmail = nextString();
			Integer newQuotaLimit = 123;
			Integer newQuotaUsed = 321;

			user.setName(newName);
			user.setId(newUserId);
			user.setEmail(newEmail);
			user.setQuotaLimit(newQuotaLimit);
			user.setQuotaUsed(newQuotaUsed);

			userDao.add(user);

			UserRMI user2 = userDao.findById(id);
			assertEquals(user, user2);
		}
	}

	@Test
	public void testGetNonExistingUserById() throws RemoteException {
		UserRMI user = userDao.findById(UUID.randomUUID());

		if (user == null) {
			assertTrue(true);
		} else {
			assertTrue("User should not exist", false);
		}
	}

	@Test
	public void testGetExistingUserById() throws IllegalArgumentException,
			DAOException, RemoteException {

		UserRMI user = new UserRMI();
		user.setName(nextString());
		user.setId(UUID.randomUUID());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {

			UserRMI user2 = userDao.findById(user.getId());

			if (user2 == null) {
				assertTrue("User should exist", false);
			} else {
				if (user2.getId() != null && user2.isValid()) {
					assertTrue(true);
				} else {
					assertTrue("User is not valid", false);
				}
			}
		}
	}
}
