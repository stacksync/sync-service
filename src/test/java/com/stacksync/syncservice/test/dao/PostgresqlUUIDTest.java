/**
 * 
 */
package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.junit.Test;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class PostgresqlUUIDTest {

	@Test
	public void test() throws Exception {
		String HOST = "10.30.233.113";
		int PORT = 5432;
		String DB = "testdb";
		String USER = "testos";
		String PASS = "testos";

		Class.forName("org.postgresql.Driver");
		Connection connection = DriverManager.getConnection("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB, USER, PASS);

		String dataSource = "postgresql";
		DAOFactory factory = new DAOFactory(dataSource);

		UUID userId = UUID.randomUUID();
		String idStr = userId.toString();

		User user = new User();
		user.setName(idStr);
		user.setId(UUID.randomUUID());
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

		UserDAO userDao = factory.getUserDao(connection);
		DeviceDAO deviceDao = factory.getDeviceDAO(connection);
		WorkspaceDAO workspaceDao = factory.getWorkspaceDao(connection);

		userDao.add(user);
		deviceDao.add(device);
		workspaceDao.add(workspace);

		Workspace w = workspaceDao.getById(userId, userId);
		assertEquals(userId, w.getId());

		Device d = deviceDao.get(userId, userId);
		assertEquals(userId, d.getId());

		userDao.delete(user.getId());
		connection.close();

	}

}
