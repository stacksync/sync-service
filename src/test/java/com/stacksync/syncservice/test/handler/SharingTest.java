package com.stacksync.syncservice.test.handler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.util.Config;

public class SharingTest {

	private static Handler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static User user1;
	private static User user2;

	@BeforeClass
	public static void initializeData() {

		try {
			Config.loadProperties();

			String datasource = Config.getDatasource();
			ConnectionPool pool = ConnectionPoolFactory
					.getConnectionPool(datasource);

			handler = new SQLHandler(pool);
			DAOFactory factory = new DAOFactory(datasource);

			Connection connection = pool.getConnection();

			workspaceDAO = factory.getWorkspaceDao(connection);
			userDao = factory.getUserDao(connection);

			user1 = new User(null, "junituser", "aa", "aa", 1000, 100);
			try {
				userDao.add(user1);
				Workspace workspace1 = new Workspace(null, 1,
						user1, false);
				workspaceDAO.add(workspace1);
			} catch (DAOException e) {
				System.out.println("User already exists.");
				user1 = userDao.findByCloudId("aa");
			}
			
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void cleanData() throws DAOException {
		// userDao.delete("aa");
	}

	@Test
	public void createShareProposal() throws DAOException, ShareProposalNotCreatedException, UserNotFoundException {

		user2 = new User(null, "junituser", "bb", "user2@users.com", 1000, 100);
		try {
			userDao.add(user2);
		} catch (DAOException e) {
			System.out.println("User already exists.");
			user2 = userDao.findByCloudId("bb");
		}
		
		List<String> emails = new ArrayList<String>();
		emails.add(user2.getEmail());
		emails.add("fakemail@fake.com");

		
		Workspace result = handler.doCreateShareProposal(user1, emails, "shared_folder");

		System.out.println("Result: " + result.getId() );

		assertNotEquals(-1L, result.getId().longValue());
	}
	
	@Test (expected = ShareProposalNotCreatedException.class)
	public void createShareProposalFakeMail() throws DAOException, ShareProposalNotCreatedException, UserNotFoundException {

		user2 = new User(null, "junituser", "bb", "user2@users.com", 1000, 100);
		try {
			userDao.add(user2);
		} catch (DAOException e) {
			System.out.println("User already exists.");
			user2 = userDao.findByCloudId("bb");
		}
		
		List<String> emails = new ArrayList<String>();
		emails.add("fakemail@fake.com");

		
		Workspace result = handler.doCreateShareProposal(user1, emails, "shared_folder");

		System.out.println("Result: " + result.getId() );
	}

}
