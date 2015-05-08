package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
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
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Config;

public class PostgresqlDAOTest {

//	private static Connection connection;
//	private static WorkspaceDAO workspaceDAO;
//	private static UserDAO userDao;
//	private static ItemDAO objectDao;
//	private static ItemVersionDAO oversionDao;
//	private static SecureRandom random = new SecureRandom();
//
//	@BeforeClass
//	public static void testSetup() throws IOException, SQLException, DAOConfigurationException {
//
//		URL configFileResource = PostgresqlDAOTest.class.getResource("/com/ast/processserver/resources/log4j.xml");
//		DOMConfigurator.configure(configFileResource);
//
//		Config.loadProperties();
//
//		String dataSource = "postgresql";
//		DAOFactory factory = new DAOFactory(dataSource);
//		connection = ConnectionPoolFactory.getConnectionPool(dataSource).getConnection();
//		workspaceDAO = factory.getWorkspaceDao(connection);
//		userDao = factory.getUserDao(connection);
//		objectDao = factory.getItemDAO(connection);
//		oversionDao = factory.getItemVersionDAO(connection);
//	}
//
//	protected String nextString() {
//		return new BigInteger(130, random).toString(32);
//	}
//
//	@Test
//	public void testCreateNewValidUser() throws IllegalArgumentException, DAOException {
//		User user = new User();
//		user.setName(nextString());
//		user.setId(UUID.randomUUID());
//		user.setEmail(nextString());
//		user.setQuotaLimit(2048);
//		user.setQuotaUsed(1403);
//
//		userDao.add(user);
//
//		if (user.getId() == null) {
//			assertTrue("Could not retrieve the User ID", false);
//		} else {
//			assertTrue(true);
//		}
//
//	}
//
//	@Test
//	public void testCreateNewUserSameId() throws IllegalArgumentException, DAOException {
//
//		UUID userId = UUID.randomUUID();
//
//		User user = new User();
//		user.setName(nextString());
//		user.setId(userId);
//		user.setEmail(nextString());
//		user.setQuotaLimit(2048);
//		user.setQuotaUsed(1403);
//
//		userDao.add(user);
//
//		if (user.getId() == null) {
//			assertTrue("Could not retrieve the User ID", false);
//		} else {
//			User user2 = new User();
//			user2.setName(nextString());
//			user2.setId(userId);
//			user2.setEmail(nextString());
//			user2.setQuotaLimit(2048);
//			user2.setQuotaUsed(1403);
//
//			try {
//				userDao.add(user2);
//				assertTrue("User should not have been created", false);
//			} catch (DAOException e) {
//				assertTrue(e.toString(), true);
//			}
//		}
//	}
//
//	@Test
//	public void testUpdateExistingUserOk() throws IllegalArgumentException, DAOException {
//
//		User user = new User();
//		user.setName(nextString());
//		user.setId(UUID.randomUUID());
//		user.setEmail(nextString());
//		user.setQuotaLimit(2048);
//		user.setQuotaUsed(1403);
//
//		userDao.add(user);
//
//		if (user.getId() == null) {
//			assertTrue("Could not retrieve the User ID", false);
//		} else {
//
//			UUID id = user.getId();
//			String newName = nextString();
//			UUID newUserId = UUID.randomUUID();
//			String newEmail = nextString();
//			Integer newQuotaLimit = 123;
//			Integer newQuotaUsed = 321;
//
//			user.setName(newName);
//			user.setId(newUserId);
//			user.setEmail(newEmail);
//			user.setQuotaLimit(newQuotaLimit);
//			user.setQuotaUsed(newQuotaUsed);
//
//			try {
//				userDao.add(user);
//
//				User user2 = userDao.findById(id);
//				assertEquals(user, user2);
//
//			} catch (DAOException e) {
//				assertTrue(e.toString(), false);
//			}
//		}
//	}
//
//	@Test
//	public void testGetNonExistingUserById() {
//		try {
//			User user = userDao.findById(UUID.randomUUID());
//
//			if (user == null) {
//				assertTrue(true);
//			} else {
//				assertTrue("User should not exist", false);
//			}
//
//		} catch (DAOException e) {
//			assertTrue(e.toString(), false);
//		}
//	}
//
//	@Test
//	public void testGetExistingUserById() throws IllegalArgumentException, DAOException {
//
//		User user = new User();
//		user.setName(nextString());
//		user.setId(UUID.randomUUID());
//		user.setEmail(nextString());
//		user.setQuotaLimit(2048);
//		user.setQuotaUsed(1403);
//
//		userDao.add(user);
//
//		if (user.getId() == null) {
//			assertTrue("Could not retrieve the User ID", false);
//		} else {
//
//			try {
//				User user2 = userDao.findById(user.getId());
//
//				if (user2 == null) {
//					assertTrue("User should exist", false);
//				} else {
//					if (user2.getId() != null && user2.isValid()) {
//						assertTrue(true);
//					} else {
//						assertTrue("User is not valid", false);
//					}
//				}
//
//			} catch (DAOException e) {
//				assertTrue(e.toString(), false);
//			}
//		}
//	}
//
//	@Test
//	public void testCreateNewWorkspaceInvalidOwner() {
//
//		User user = new User(UUID.randomUUID());
//
//		Workspace workspace = new Workspace();
//		workspace.setOwner(user);
//
//		try {
//			workspaceDAO.add(workspace);
//			assertTrue("User should not have been created", false);
//		} catch (DAOException e) {
//			assertTrue(e.toString(), true);
//		}
//	}
//
//	@Test
//	public void testCreateNewWorkspaceValidOwner() throws IllegalArgumentException, DAOException {
//
//		User user = new User();
//		user.setName(nextString());
//		user.setId(UUID.randomUUID());
//		user.setEmail(nextString());
//		user.setQuotaLimit(2048);
//		user.setQuotaUsed(1403);
//		userDao.add(user);
//
//		Workspace workspace = new Workspace();
//		workspace.setLatestRevision(0);
//		workspace.setOwner(user);
//
//		try {
//			workspaceDAO.add(workspace);
//			assertTrue(true);
//		} catch (DAOException e) {
//			assertTrue(e.toString(), false);
//		}
//	}
//
//	@Test
//	public void testCreateObjectInvalidWorkspace() throws IllegalArgumentException, DAOException {
//
//		User user = new User();
//		user.setName(nextString());
//		user.setId(UUID.randomUUID());
//		user.setEmail(nextString());
//		user.setQuotaLimit(2048);
//		user.setQuotaUsed(1403);
//		userDao.add(user);
//
//		Workspace workspace = new Workspace();
//		workspace.setOwner(user);
//		workspace.setLatestRevision(0);
//
//		Item object = new Item();
//		object.setWorkspace(workspace);
//		object.setLatestVersion(1L);
//		object.setParent(null);
//		object.setId(1331432L);
//		object.setFilename(nextString());
//		object.setMimetype("image/jpeg");
//		object.setIsFolder(false);
//		object.setClientParentFileVersion(1L);
//
//		try {
//			objectDao.put(object);
//			assertTrue("Object should not have been created", false);
//		} catch (DAOException e) {
//			assertTrue(e.toString(), true);
//		}
//
//	}
//
//
//	@Test
//	public void testGetObjectByClientFileIdAndWorkspace() throws DAOException {
//
//		long fileId = 4852407995043916970L;
//		objectDao.findById(fileId);
//
//		// TODO Check if the returned obj is correct
//	}
//
//	@Test
//	public void testGetWorkspaceById() {
//
//	}
//
//	@Test
//	public void testGetObjectMetadataByWorkspaceName() throws DAOException {
//
//		List<ItemMetadata> objects = objectDao.getItemsByWorkspaceId(UUID.randomUUID());
//
//		if (objects != null && !objects.isEmpty()) {
//
//			for (ItemMetadata object : objects) {
//				System.out.println(object.toString());
//			}
//
//			assertTrue(true);
//		} else {
//			assertTrue(false);
//		}
//	}
//
//	@Test
//	public void testGetObjectMetadataByClientFileIdWithoutChunks() throws DAOException {
//
//		Long fileId = 538757639L;
//		boolean includeDeleted = false;
//		boolean includeChunks = false;
//		Long version = 1L;
//		boolean list = true;
//
//		ItemMetadata object = objectDao.findById(fileId, list, version, includeDeleted, includeChunks);
//
//		if (object != null) {
//			System.out.println(object.toString());
//
//			if (object.getChildren() != null) {
//				for (ItemMetadata child : object.getChildren()) {
//					System.out.println(child.toString());
//				}
//			}
//			assertTrue(true);
//		} else {
//			assertTrue(false);
//		}
//	}
//
//	@Test
//	public void testGetObjectMetadataByClientFileIdWithChunks() throws DAOException {
//
//		Long fileId = 538757639L;
//		boolean includeDeleted = false;
//		boolean includeChunks = true;
//		Long version = 1L;
//		boolean list = true;
//
//		ItemMetadata object = objectDao.findById(fileId, list, version, includeDeleted, includeChunks);
//
//		if (object != null) {
//			System.out.println(object.toString());
//
//			if (object.getChildren() != null) {
//				for (ItemMetadata child : object.getChildren()) {
//					System.out.println(child.toString());
//				}
//			}
//			assertTrue(true);
//		} else {
//			assertTrue(false);
//		}
//	}
//
//	@Test
//	public void testGetObjectMetadataByServerUserId() throws DAOException {
//
//		UUID userId = UUID.randomUUID();
//		boolean includeDeleted = false;
//
//		ItemMetadata object = objectDao.findByUserId(userId, includeDeleted);
//
//		if (object != null) {
//			System.out.println(object.toString());
//
//			if (object.getChildren() != null) {
//				for (ItemMetadata child : object.getChildren()) {
//					System.out.println(child.toString());
//				}
//			}
//			assertTrue(true);
//		} else {
//			assertTrue(false);
//		}
//	}

}
