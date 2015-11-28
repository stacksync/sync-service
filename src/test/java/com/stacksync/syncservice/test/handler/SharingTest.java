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

public class SharingTest {

	private static Handler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static UserRMI user1;
	private static UserRMI user2;
	private static WorkspaceRMI workspace1;

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
		workspace1 = new WorkspaceRMI(null, 1, user1, false, false);
		workspaceDAO.add(workspace1);

	}

	@AfterClass
	public static void cleanData() throws DAOException {
		// userDao.delete("aa");
	}

	/*@Test
	public void createShareProposal() throws DAOException, ShareProposalNotCreatedException, UserNotFoundException {

		user2 = new User(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

		userDao.add(user2);

		List<String> emails = new ArrayList<String>();
		emails.add(user2.getEmail());
		emails.add("fakemail@fake.com");

		Workspace result = handler.doCreateShareProposal (user1, emails, "shared_folder", false);

		System.out.println("Result: " + result.getId());

		assertNotEquals("-1", result.getId());
	}

	@Test(expected = ShareProposalNotCreatedException.class)
	public void createShareProposalFakeMail() throws DAOException, ShareProposalNotCreatedException,
			UserNotFoundException {

		user2 = new User(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
		userDao.add(user2);


		List<String> emails = new ArrayList<String>();
		emails.add("fakemail@fake.com");

		Workspace result = handler.doCreateShareProposal(user1, emails, "shared_folder", false);

		System.out.println("Result: " + result.getId());
	}

	@Test
	public void updateWorkspace() throws UserNotFoundException, WorkspaceNotUpdatedException {

		workspace1.setName("workspace_folder");
		workspace1.setParentItem(null);

		handler.doUpdateWorkspace(user1, workspace1);
	}*/

}
