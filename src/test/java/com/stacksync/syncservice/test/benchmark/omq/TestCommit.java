package com.stacksync.syncservice.test.benchmark.omq;

import com.stacksync.commons.omq.ISyncService;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.test.benchmark.normal.CommonFunctions;
import com.stacksync.syncservice.util.Config;
import omq.common.broker.Broker;

import java.util.Properties;

public class TestCommit {

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		
		Properties env = RabbitConfig.getProperties();

		Broker broker = new Broker(env);
		broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);

		CommonFunctions.generateObjects(1, Constants.DEVICE_ID);

		DAOFactory factory = new DAOFactory(datasource);
		WorkspaceDAO workspaceDao = factory.getDAO(pool.getConnection());

		WorkspaceRMI workspace = workspaceDao.getById(Constants.WORKSPACE_ID);

		long startTotal = System.currentTimeMillis();
		// server.commit(Constants.USER, Constants.REQUESTID, rWorkspace,
		// Constants.DEVICENAME, metadata);

		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
