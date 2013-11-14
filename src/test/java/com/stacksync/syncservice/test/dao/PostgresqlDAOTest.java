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

import org.apache.log4j.xml.DOMConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.Object1DAO;
import com.stacksync.syncservice.db.ObjectVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.model.ObjectVersion;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.util.Config;

public class PostgresqlDAOTest {

	private static Connection connection;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static Object1DAO objectDao;
	private static ObjectVersionDAO oversionDao;
	private static SecureRandom random = new SecureRandom();

	@BeforeClass
	public static void testSetup() throws IOException, SQLException, DAOConfigurationException {

		URL configFileResource = PostgresqlDAOTest.class.getResource("/com/ast/processserver/resources/log4j.xml");
		DOMConfigurator.configure(configFileResource);

		Config.loadProperties();

		String dataSource = "postgresql";
		DAOFactory factory = new DAOFactory(dataSource);
		connection = ConnectionPoolFactory.getConnectionPool(dataSource).getConnection();
		workspaceDAO = factory.getWorkspaceDao(connection);
		userDao = factory.getUserDao(connection);
		objectDao = factory.getObject1DAO(connection);
		oversionDao = factory.getObjectVersionDAO(connection);
	}

	protected String nextString() {
		return new BigInteger(130, random).toString(32);
	}

	@Test
	public void testCreateNewValidUser() throws IllegalArgumentException, DAOException {
		User user = new User();
		user.setName(nextString());
		user.setCloudId(nextString());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {
			assertTrue(true);
		}

	}

	@Test
	public void testCreateNewUserSameCloudId() throws IllegalArgumentException, DAOException {

		String cloudId = nextString();

		User user = new User();
		user.setName(nextString());
		user.setCloudId(cloudId);
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {
			User user2 = new User();
			user2.setName(nextString());
			user2.setCloudId(cloudId);
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
	public void testUpdateExistingUserOk() throws IllegalArgumentException, DAOException {

		User user = new User();
		user.setName(nextString());
		user.setCloudId(nextString());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {

			Long id = user.getId();
			String newName = nextString();
			String newCloudId = nextString();
			String newEmail = nextString();
			Integer newQuotaLimit = 123;
			Integer newQuotaUsed = 321;

			user.setName(newName);
			user.setCloudId(newCloudId);
			user.setEmail(newEmail);
			user.setQuotaLimit(newQuotaLimit);
			user.setQuotaUsed(newQuotaUsed);

			try {
				userDao.add(user);

				User user2 = userDao.findByPrimaryKey(id);
				assertEquals(user, user2);

			} catch (DAOException e) {
				assertTrue(e.toString(), false);
			}
		}
	}

	@Test
	public void testGetNonExistingUserById() {
		try {
			User user = userDao.findByPrimaryKey(999999L);

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
	public void testGetExistingUserById() throws IllegalArgumentException, DAOException {

		User user = new User();
		user.setName(nextString());
		user.setCloudId(nextString());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		if (user.getId() == null) {
			assertTrue("Could not retrieve the User ID", false);
		} else {

			try {
				User user2 = userDao.findByPrimaryKey(user.getId());

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

	@Test
	public void testCreateNewWorkspaceInvalidOwner() {

		User user = new User(12345L);

		Workspace workspace = new Workspace();
		workspace.setClientWorkspaceName(nextString());
		workspace.setOwner(user);

		try {
			workspaceDAO.add(workspace);
			assertTrue("User should not have been created", false);
		} catch (DAOException e) {
			assertTrue(e.toString(), true);
		}
	}

	@Test
	public void testCreateNewWorkspaceValidOwner() throws IllegalArgumentException, DAOException {

		User user = new User();
		user.setName(nextString());
		user.setCloudId(nextString());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);
		userDao.add(user);

		Workspace workspace = new Workspace();
		workspace.setClientWorkspaceName(nextString());
		workspace.setLatestRevision(0);
		workspace.setOwner(user);

		try {
			workspaceDAO.add(workspace);
			assertTrue(true);
		} catch (DAOException e) {
			assertTrue(e.toString(), false);
		}
	}

	@Test
	public void testCreateObjectInvalidWorkspace() throws IllegalArgumentException, DAOException {

		User user = new User();
		user.setName(nextString());
		user.setCloudId(nextString());
		user.setEmail(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);
		userDao.add(user);

		Workspace workspace = new Workspace();
		workspace.setClientWorkspaceName(nextString());
		workspace.setOwner(user);
		workspace.setLatestRevision(0);

		Object1 object = new Object1();
		object.setWorkspace(workspace);
		object.setLatestVersion(1L);
		object.setParent(null);
		object.setClientFileId(1331432L);
		object.setClientFileName(nextString());
		object.setClientFileMimetype("image/jpeg");
		object.setRootId("blabla");
		object.setClientFolder(false);
		object.setClientParentFileId(0L);
		object.setClientParentFileVersion(1L);
		object.setClientParentRootId("blabla2");

		try {
			objectDao.put(object);
			assertTrue("Object should not have been created", false);
		} catch (DAOException e) {
			assertTrue(e.toString(), true);
		}

	}

	@Test
	public void testGetWorkspaceIdByName() throws DAOException {
		System.out.println(workspaceDAO.getPrimaryKey("cotes_workspace"));
	}

	@Test
	public void testGetObjectByWorkspaceId() throws DAOException {

		List<Object1> objects = objectDao.findByWorkspaceId(1);

		if (objects != null && !objects.isEmpty()) {

			/*
			 * for (Object1 object : objects) {
			 * System.out.println(object.toString()); }
			 */
			assertTrue(true);

		} else {
			assertTrue(false);
		}

	}

	@Test
	public void testGetObjectVersionByObjectIdAndVersion() throws DAOException {

		List<Object1> objects = objectDao.findByWorkspaceId(1);

		ObjectVersion objectVersion = oversionDao.findByObjectIdAndVersion(objects.get(0).getId(), objects.get(0).getLatestVersion());

		if (objectVersion != null) {
			System.out.println(objectVersion.toString());
			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

	@Test
	public void testGetChunksByObjectVersionId() throws DAOException {

		List<Object1> objects = objectDao.findByWorkspaceId(1);

		ObjectVersion objectVersion = oversionDao.findByObjectIdAndVersion(objects.get(0).getId(), objects.get(0).getLatestVersion());

		List<Chunk> chunks = oversionDao.findChunks(objectVersion.getId());

		if (chunks != null && !chunks.isEmpty()) {

			for (Chunk chunk : chunks) {
				System.out.println(chunk.toString());
			}

			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

	@Test
	public void testCreateObjectVersionExistingObjectIdAndVersion() {

	}

	@Test
	public void testGetObjectByClientFileIdAndWorkspace() throws DAOException {

		long fileId = 4852407995043916970L;
		objectDao.findByClientFileIdAndWorkspace(fileId, 2L);

		// TODO Check if the returned obj is correct
	}

	@Test
	public void testGetWorkspaceById() {

	}

	@Test
	public void testGetObjectsByWorkspaceName() throws DAOException {

		List<Object1> objects = objectDao.findByWorkspaceName("benchmark/");

		if (objects != null && !objects.isEmpty()) {

			for (Object1 object : objects) {
				System.out.println(object.toString());
			}

			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

	@Test
	public void testGetObjectMetadataByWorkspaceName() throws DAOException {

		List<ObjectMetadata> objects = objectDao.getObjectMetadataByWorkspaceName("benchmark/");

		if (objects != null && !objects.isEmpty()) {

			for (ObjectMetadata object : objects) {
				System.out.println(object.toString());
			}

			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

	@Test
	public void testGetObjectMetadataByClientFileIdWithoutChunks() throws DAOException {

		Long fileId = 538757639L;
		boolean includeDeleted = false;
		boolean includeChunks = false;
		Long version = 1L;
		boolean list = true;

		ObjectMetadata object = objectDao.findByClientFileId(fileId, list, version, includeDeleted, includeChunks);

		if (object != null) {
			System.out.println(object.toString());

			if (object.getContent() != null) {
				for (ObjectMetadata child : object.getContent()) {
					System.out.println(child.toString());
				}
			}
			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

	@Test
	public void testGetObjectMetadataByClientFileIdWithChunks() throws DAOException {

		Long fileId = 538757639L;
		boolean includeDeleted = false;
		boolean includeChunks = true;
		Long version = 1L;
		boolean list = true;

		ObjectMetadata object = objectDao.findByClientFileId(fileId, list, version, includeDeleted, includeChunks);

		if (object != null) {
			System.out.println(object.toString());

			if (object.getContent() != null) {
				for (ObjectMetadata child : object.getContent()) {
					System.out.println(child.toString());
				}
			}
			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

	@Test
	public void testGetObjectMetadataByServerUserId() throws DAOException {

		String serverUserId = "bb";
		boolean includeDeleted = false;

		ObjectMetadata object = objectDao.findByServerUserId(serverUserId, includeDeleted);

		if (object != null) {
			System.out.println(object.toString());

			if (object.getContent() != null) {
				for (ObjectMetadata child : object.getContent()) {
					System.out.println(child.toString());
				}
			}
			assertTrue(true);
		} else {
			assertTrue(false);
		}
	}

}
