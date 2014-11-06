package com.stacksync.syncservice.test.rmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.xml.DOMConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.rmiserver.*;
import com.stacksync.syncservice.util.Config;

public class RMITest {

	private static Connection connection;
	private static WorkspaceDAORMISer workspaceDAO;
	private static UserDAORMISer userDao;
	private static ItemDAORMISer objectDao;
	private static ItemVersionDAORMISer oversionDao;
	private static SecureRandom random = new SecureRandom();

	@BeforeClass
	public static void testSetup() throws IOException, SQLException,
			DAOConfigurationException {

		Config.loadProperties();

		String dataSource = "postgresql";
		DAOFactoryRMISer factory = new DAOFactoryRMISer(dataSource);
		connection = ConnectionPoolFactoryRMISer.getConnectionPool(dataSource).getConnection();
		workspaceDAO = factory.getWorkspaceDao();
		if (factory.getUserDao() != null) {
			userDao = factory.getUserDao();
		} else
			System.out.println("UserDAO --> NULL");
		objectDao = factory.getItemDAO();
		oversionDao = factory.getItemVersionDAO();

		try {
			LocateRegistry.createRegistry(1099);
			PostgresqlUserDAORMISer addPostgresqlUserDAO = new PostgresqlUserDAORMISer();
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
			DAOException {
		User user = new User();
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
			DAOException {

		UUID userId = UUID.randomUUID();

		User user = new User();
		user.setName(nextString());
		user.setId(userId);
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {
			User user2 = new User();
			user2.setName(nextString());
			user2.setId(userId);
			user2.setEmail(nextString());
			user2.setQuotaLimit(2048);
			user2.setQuotaUsed(1403);

			try {
				userDao.add(user2);
				assertTrue("User should not have been created", false);
			} catch (DAOException e) {
				assertTrue(e.toString(), true);
			}
		}
	}

	@Test
	public void testUpdateExistingUserOk() throws IllegalArgumentException,
			DAOException {

		User user = new User();
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

			try {
				userDao.add(user);

				User user2 = userDao.findById(id);
				assertEquals(user, user2);

			} catch (DAOException e) {
				assertTrue(e.toString(), false);
			}
		}
	}

	@Test
	public void testGetNonExistingUserById() {
		try {
			User user = userDao.findById(UUID.randomUUID());

			if (user == null) {
				assertTrue(true);
			} else {
				assertTrue("User should not exist", false);
			}

		} catch (DAOException e) {
			assertTrue(e.toString(), false);
		}
	}

	@Test
	public void testGetExistingUserById() throws IllegalArgumentException,
			DAOException {

		User user = new User();
		user.setName(nextString());
		user.setId(UUID.randomUUID());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {

			try {
				User user2 = userDao.findById(user.getId());

				if (user2 == null) {
					assertTrue("User should exist", false);
				} else {
					if (user2.getId() != null && user2.isValid()) {
						assertTrue(true);
					} else {
						assertTrue("User is not valid", false);
					}
				}

			} catch (DAOException e) {
				assertTrue(e.toString(), false);
			}
		}
	}
}
