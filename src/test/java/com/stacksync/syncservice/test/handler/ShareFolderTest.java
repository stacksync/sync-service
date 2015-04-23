package com.stacksync.syncservice.test.handler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.handler.APIHandler;
import com.stacksync.syncservice.handler.SQLAPIHandler;
import com.stacksync.syncservice.handler.Handler.Status;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.util.Config;
import com.stacksync.syncservice.util.Constants;

public class ShareFolderTest {

	private static SQLSyncHandler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static User user1;
	private static User user2;

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

		user1 = new User(UUID.fromString("159a1286-33df-4453-bf80-cff4af0d97b0"), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100L, 0L, 0L, true);
		
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
		Item item = new Item(125L);
		
		handler.doShareFolder(user1, emails, item, false);
		
	}

}
