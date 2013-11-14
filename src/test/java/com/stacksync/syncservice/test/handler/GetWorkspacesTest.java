package com.stacksync.syncservice.test.handler;

import java.sql.Connection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.rpc.messages.GetWorkspaces;
import com.stacksync.syncservice.rpc.messages.GetWorkspacesResponse;
import com.stacksync.syncservice.rpc.parser.IParser;
import com.stacksync.syncservice.rpc.parser.JSONParser;
import com.stacksync.syncservice.util.Config;

public class GetWorkspacesTest {

	private static IParser reader;
	private static Handler handler;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static DeviceDAO deviceDao;

	@BeforeClass
	public static void initializeData() {

		try {
			Config.loadProperties();
			reader = new JSONParser();

			String datasource = Config.getDatasource();
			ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

			handler = new SQLHandler(pool);
			DAOFactory factory = new DAOFactory(datasource);

			Connection connection = pool.getConnection();

			workspaceDAO = factory.getWorkspaceDao(connection);
			userDao = factory.getUserDao(connection);
			deviceDao = factory.getDeviceDAO(connection);

			User user = new User(null, "junituser", "aa", "aa", 1000, 100);
			userDao.add(user);

			Workspace workspace1 = new Workspace(null, "junituser1/", 1, user);
			workspaceDAO.add(workspace1);

			Workspace workspace2 = new Workspace(null, "junituser2/", 1, user);
			workspaceDAO.add(workspace2);

			Workspace workspace3 = new Workspace(null, "junituser3/", 1, user);
			workspaceDAO.add(workspace3);

			Device device = new Device(null, "junitdevice", user);
			deviceDao.add(device);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void cleanData() throws DAOException {
		userDao.delete("aa");
	}

	@Test
	public void getWorkspaces() throws DAOException {

		GetWorkspaces msg = new GetWorkspaces("111", "aa", "");
		GetWorkspacesResponse response = handler.doGetWorkspaces(msg);

		printResponse(response);

	}

	private void printResponse(GetWorkspacesResponse r) {
		String response = reader.createResponse(r);
		System.out.println(response);
	}

}
