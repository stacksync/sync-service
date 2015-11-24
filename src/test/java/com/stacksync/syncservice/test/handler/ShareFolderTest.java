package com.stacksync.syncservice.test.handler;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShareFolderTest {

	private static SQLSyncHandler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static UserRMI user1;
	private static UserRMI user2;

	@BeforeClass
	public static void initializeData() throws Exception {

		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

		handler = new SQLSyncHandler(pool);
		DAOFactory factory = new DAOFactory(datasource);

		Connection connection = pool.getConnection();

		
		workspaceDAO = factory.getWorkspaceDao(connection);
		userDao = factory.getUserDao(connection);

		user1 = new UserRMI(UUID.fromString("159a1286-33df-4453-bf80-cff4af0d97b0"), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
		
		/*
		userDao.add(user1);
		Workspace workspace1 = new Workspace(null, 1, user1, false, false);
		workspaceDAO.add(workspace1);

		user2 = new User(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

		userDao.add(user2);
		Workspace workspace2 = new Workspace(null, 1, user2, false, false);
		workspaceDAO.add(workspace2);
		*/
	}
	
	@Test
	public void shareFolder() throws Exception {
		
		List<String> emails = new ArrayList<String>();
		emails.add("c@c.c");
		ItemRMI item = new ItemRMI(125L);
		
		handler.doShareFolder(user1, emails, item, false);
		
	}

}
