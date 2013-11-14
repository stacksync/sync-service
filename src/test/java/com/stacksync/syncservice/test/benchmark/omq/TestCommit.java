package com.stacksync.syncservice.test.benchmark.omq;

import java.util.Properties;

import omq.common.broker.Broker;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.WorkspaceInfo;
import com.stacksync.syncservice.omq.ISyncService;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.test.benchmark.normal.CommonFunctions;
import com.stacksync.syncservice.util.Config;

public class TestCommit {

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		
		Properties env = RabbitConfig.getProperties();

		Broker broker = new Broker(env);
		broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);

		CommonFunctions.generateObjects(1, Constants.DEVICENAME);

		DAOFactory factory = new DAOFactory(datasource);
		WorkspaceDAO workspaceDao = factory.getWorkspaceDao(pool.getConnection());

		Workspace workspace = workspaceDao.findByName(Constants.WORKSPACEID);
		new WorkspaceInfo(workspace.getClientWorkspaceName(), workspace.getLatestRevision(), "/");

		long startTotal = System.currentTimeMillis();
		// server.commit(Constants.USER, Constants.REQUESTID, rWorkspace,
		// Constants.DEVICENAME, metadata);

		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
