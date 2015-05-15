/**
 * 
 */
package com.stacksync.syncservice.dummy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.util.UUID;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Config;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class FillDBWithUsers {
	// 584316987,558432e6-c0d9-3843-b2aa-11b08ff90a65

	private ConnectionPool pool;
	private UserDAO userDao;
	private WorkspaceDAO workspaceDao;
	private DeviceDAO deviceDao;

	private FillDBWithUsers() throws Exception {
		String configPath = "config2.properties";
		Config.loadProperties(configPath);
		String datasource = Config.getDatasource();
		pool = ConnectionPoolFactory.getConnectionPool(datasource);
		Connection conn = pool.getConnection();

		DAOFactory factory = new DAOFactory(datasource);
		userDao = factory.getUserDao(conn);
		deviceDao = factory.getDeviceDAO(conn);
		workspaceDao = factory.getWorkspaceDao(conn);
	}

	private void createUser(UUID userId) throws DAOException {
		String idStr = userId.toString();

		User user = new User();
		user.setName(idStr);
		user.setId(userId);
		user.setEmail(idStr);
		user.setSwiftUser(idStr);
		user.setSwiftAccount(idStr);
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		Device device = new Device();
		device.setId(userId);
		device.setAppVersion("1");
		device.setLastIp("192.168.1.1");
		device.setName("1+1");
		device.setOs("Android");
		device.setUser(user);

		Workspace workspace = new Workspace();
		workspace.setId(userId);
		workspace.setLatestRevision(0);
		workspace.setOwner(user);

		userDao.add(user);
		deviceDao.add(device);
		workspaceDao.add(workspace);
	}

	public static void main(String[] args) throws Exception {
		args = new String[] { "day_users.csv" };

		if (args.length != 1) {
			System.err.println("Usage: file_path");
			System.exit(0);
		}

		FillDBWithUsers filler = new FillDBWithUsers();

		String line;
		BufferedReader buff = new BufferedReader(new FileReader(new File(args[0])));
		while ((line = buff.readLine()) != null) {
			String id = line.split(",")[1];
			filler.createUser(UUID.fromString(id));
		}

		buff.close();
	}
}
