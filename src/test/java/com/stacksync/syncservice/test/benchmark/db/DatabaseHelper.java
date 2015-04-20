package com.stacksync.syncservice.test.benchmark.db;

import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanDeviceDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanItemDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanItemVersionDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanUserDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanWorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemVersionRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Config;
import java.rmi.RemoteException;

public class DatabaseHelper {
	private ConnectionPool pool;
	private Connection connection;
	private InfinispanWorkspaceDAO workspaceDAO;
	private InfinispanUserDAO userDao;
	private InfinispanDeviceDAO deviceDao;
	private InfinispanItemDAO objectDao;
	private InfinispanItemVersionDAO oversionDao;

	public DatabaseHelper() throws Exception {
		Config.loadProperties();
		Thread.sleep(100);

		String datasource = Config.getDatasource();

		pool = ConnectionPoolFactory.getConnectionPool(datasource);
		connection = pool.getConnection();

		DAOFactory factory = new DAOFactory(datasource);

		workspaceDAO = factory.getWorkspaceDao(connection);
		userDao = factory.getUserDao(connection);
		deviceDao = factory.getDeviceDAO(connection);
		objectDao = factory.getItemDAO(connection);
		oversionDao = factory.getItemVersionDAO(connection);
	}

	public void storeObjects(List<ItemRMI> objectsLevel) throws IllegalArgumentException, DAOException, RemoteException {

		long numChunk = 0, totalTimeChunk = 0;
		long numVersion = 0, totalTimeVersion = 0;
		long numObject = 0, totalTimeObject = 0;

		long startTotal = System.currentTimeMillis();
		for (ItemRMI object : objectsLevel) {
			// System.out.println("DatabaseHelper -- Put Object -> " + object);
			long startObjectTotal = System.currentTimeMillis();

			objectDao.put(object);
			for (ItemVersionRMI version : object.getVersions()) {

				long startVersionTotal = System.currentTimeMillis();
				// System.out.println("DatabaseHelper -- Put Version -> " +
				// version);
				oversionDao.add(version);

				long startChunkTotal = System.currentTimeMillis();

				if (!version.getChunks().isEmpty()) {
					oversionDao.insertChunks(version.getItemId(), version.getChunks(), version.getId());
				}

				totalTimeChunk += System.currentTimeMillis() - startChunkTotal;

				totalTimeVersion += System.currentTimeMillis() - startVersionTotal;
				// System.out.println("---- Total Version time --> " +
				// totalVersionTime + " ms");
				numVersion++;
			}

			totalTimeObject += System.currentTimeMillis() - startObjectTotal;
			numObject++;
		}

		if (numChunk > 0) {
			System.out.println("-------- AVG avg Chunk(" + numChunk + ") time --> " + (totalTimeChunk / numChunk) + " ms");
		}

		if (numVersion > 0) {
			System.out.println("---- AVG Version(" + numVersion + ") time --> " + (totalTimeVersion / numVersion) + " ms");
		}

		if (numObject > 0) {
			System.out.println("AVG Object(" + numObject + ") time --> " + (totalTimeObject / numObject) + " ms");
		}

		long totalTime = System.currentTimeMillis() - startTotal;

		System.out.println("Total level time --> " + totalTime + " ms");

	}

	public void addUser(UserRMI user) throws IllegalArgumentException, DAOException, RemoteException {
		userDao.add(user);
	}

	public void addWorkspace(UserRMI user, WorkspaceRMI workspace) throws IllegalArgumentException, DAOException, RemoteException {
		workspaceDAO.add(workspace);
		workspaceDAO.addUser(user, workspace);
	}

	public void addDevice(DeviceRMI device) throws IllegalArgumentException, DAOException, RemoteException {
		deviceDao.add(device);
	}

	public void deleteUser(UUID id) throws DAOException, RemoteException {
		userDao.deleteUser(id);
	}
}
