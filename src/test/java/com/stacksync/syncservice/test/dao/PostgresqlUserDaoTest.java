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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class PostgresqlUserDaoTest {
	private static Connection connection;
	private static UserDAO userDao;
	private static User user;
	private static SecureRandom random;

	@BeforeClass
	public static void testSetup() throws ClassNotFoundException, SQLException {
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
	}

	@Before
	public void createUser() throws DAOException {
		UUID id = UUID.randomUUID();
		String idStr = id.toString();

		user = new User();
		user.setName(idStr);
		user.setId(id);
		user.setEmail(idStr);
		user.setSwiftUser(idStr);
		user.setSwiftAccount(idStr);
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);
	}

	@After
	public void removeUser() throws DAOException {
		userDao.delete(user.getId());
	}

	@Test
	public void findUserById() throws DAOException {
		UUID id = user.getId();
		User u = userDao.findById(id);

		assertEquals(id, u.getId());
	}

	@Test
	public void updateUser() throws DAOException {
		String email = "test@test.com";
		user.setEmail(email);

		userDao.update(user);

		User u = userDao.findById(user.getId());

		assertEquals(email, u.getEmail());
	}

	@Test
	public void getByEmail() throws DAOException {
		String email = user.getEmail();

		User u = userDao.getByEmail(email);

		assertEquals(user.getId(), u.getId());
	}

	@Test
	public void findAll() throws DAOException {
		List<User> users = userDao.findAll();
		for (User u : users) {
			if (u.getId().equals(user.getId())) {
				return;
			}
		}
		assertTrue(false);
	}

	public static String nextString() {
		return new BigInteger(130, random).toString(32);
	}
}
