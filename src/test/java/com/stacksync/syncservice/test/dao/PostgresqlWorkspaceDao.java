/**
 * 
 */
package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class PostgresqlWorkspaceDao {
	private static Connection connection;
	private static User user;
	private static UserDAO userDao;
	private static Workspace workspace;
	private static WorkspaceDAO workspaceDao;
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

		workspaceDao = factory.getWorkspaceDao(connection);

	}

	@AfterClass
	public static void testEnd() throws DAOException, SQLException {
		userDao.delete(user.getId());
		connection.close();
	}

	@Before
	public void createWorkspace() throws DAOException {
		workspace = new Workspace();
		workspace.setLatestRevision(0);
		workspace.setOwner(user);

		workspaceDao.add(workspace);
	}

	@After
	public void deleteWorkspace() throws DAOException {
		workspaceDao.delete(user.getId(), workspace.getId());
	}

	@Test
	public void getWorkspaceById() throws DAOException {
		UUID userId = user.getId();
		UUID workspaceId = workspace.getId();

		Workspace w = workspaceDao.getById(userId, workspaceId);

		assertEquals(workspaceId, w.getId());
	}

	@Test
	public void getWorkspaceByUserId() throws DAOException {
		UUID userId = user.getId();
		UUID workspaceId = workspace.getId();

		List<Workspace> list = workspaceDao.getByUserId(userId);

		for (Workspace w : list) {
			if (w.getId().equals(workspaceId)) {
				return;
			}
		}

		assertTrue(false);
	}

	@Test
	public void getDefaultWorkspaceByUserId() throws DAOException {
		UUID userId = user.getId();
		UUID workspaceId = workspace.getId();

		Workspace w = workspaceDao.getDefaultWorkspaceByUserId(userId);

		assertEquals(workspaceId, w.getId());
	}

	@Test
	public void updateWorkspace() throws DAOException {
		String name = "qwerty";
		workspace.setName(name);

		workspaceDao.update(user, workspace);

		Workspace w = workspaceDao.getById(user.getId(), workspace.getId());

		assertEquals(name, w.getName());

	}
	
	@Test
	public void addUser() throws DAOException{
		fail("Not yet implemented");
	}
	
	@Test
	public void getByItemId() throws DAOException{
		fail("Not yet implemented");
	}

	@Test
	public void getMembersById() {
		fail("Not yet implemented");
	}

	public static String nextString() {
		return new BigInteger(130, random).toString(32);
	}

}
