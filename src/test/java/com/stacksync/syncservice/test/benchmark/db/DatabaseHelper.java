package com.stacksync.syncservice.test.benchmark.db;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Config;

public class DatabaseHelper {
//	private ConnectionPool pool;
//	private Connection connection;
//	private WorkspaceDAO workspaceDAO;
//	private UserDAO userDao;
//	private DeviceDAO deviceDao;
//	private ItemDAO objectDao;
//	private ItemVersionDAO oversionDao;
//
//	public DatabaseHelper() throws Exception {
//		Config.loadProperties();
//		Thread.sleep(100);
//
//		String datasource = Config.getDatasource();
//
//		pool = ConnectionPoolFactory.getConnectionPool(datasource);
//		connection = pool.getConnection();
//
//		DAOFactory factory = new DAOFactory(datasource);
//
//		workspaceDAO = factory.getWorkspaceDao(connection);
//		userDao = factory.getUserDao(connection);
//		deviceDao = factory.getDeviceDAO(connection);
//		objectDao = factory.getItemDAO(connection);
//		oversionDao = factory.getItemVersionDAO(connection);
//	}
//
//	public void storeObjects(List<Item> objectsLevel) throws IllegalArgumentException, DAOException {
//
//		long numChunk = 0, totalTimeChunk = 0;
//		long numVersion = 0, totalTimeVersion = 0;
//		long numObject = 0, totalTimeObject = 0;
//
//		long startTotal = System.currentTimeMillis();
//		for (Item object : objectsLevel) {
//			// System.out.println("DatabaseHelper -- Put Object -> " + object);
//			long startObjectTotal = System.currentTimeMillis();
//
//			objectDao.put(object);
//			for (ItemVersion version : object.getVersions()) {
//
//				long startVersionTotal = System.currentTimeMillis();
//				// System.out.println("DatabaseHelper -- Put Version -> " +
//				// version);
//				oversionDao.add(version);
//
//				long startChunkTotal = System.currentTimeMillis();
//
//				if (!version.getChunks().isEmpty()) {
//					oversionDao.insertChunks(version.getChunks(), version.getId());
//				}
//
//				totalTimeChunk += System.currentTimeMillis() - startChunkTotal;
//
//				totalTimeVersion += System.currentTimeMillis() - startVersionTotal;
//				// System.out.println("---- Total Version time --> " +
//				// totalVersionTime + " ms");
//				numVersion++;
//			}
//
//			totalTimeObject += System.currentTimeMillis() - startObjectTotal;
//			numObject++;
//		}
//
//		if (numChunk > 0) {
//			System.out.println("-------- AVG avg Chunk(" + numChunk + ") time --> " + (totalTimeChunk / numChunk) + " ms");
//		}
//
//		if (numVersion > 0) {
//			System.out.println("---- AVG Version(" + numVersion + ") time --> " + (totalTimeVersion / numVersion) + " ms");
//		}
//
//		if (numObject > 0) {
//			System.out.println("AVG Object(" + numObject + ") time --> " + (totalTimeObject / numObject) + " ms");
//		}
//
//		long totalTime = System.currentTimeMillis() - startTotal;
//
//		System.out.println("Total level time --> " + totalTime + " ms");
//
//	}
//
//	public void addUser(User user) throws IllegalArgumentException, DAOException {
//		userDao.add(user);
//	}
//
//	public void addWorkspace(User user, Workspace workspace) throws IllegalArgumentException, DAOException {
//		workspaceDAO.add(workspace);
//		workspaceDAO.addUser(user, workspace);
//	}
//
//	public void addDevice(Device device) throws IllegalArgumentException, DAOException {
//		deviceDao.add(device);
//	}
//
//	public void deleteUser(UUID id) throws DAOException {
//		userDao.delete(id);
//	}
}
