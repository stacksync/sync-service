package com.stacksync.syncservice.test.benchmark.omq;

import java.util.List;
import java.util.Properties;

import omq.common.broker.Broker;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.models.WorkspaceInfo;
import com.stacksync.syncservice.omq.ISyncService;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.util.Config;

public class TestGetChanges {

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

		DAOFactory factory = new DAOFactory(datasource);
		WorkspaceDAO workspaceDao = factory.getWorkspaceDao(pool.getConnection());

		Properties env = RabbitConfig.getProperties();

		Broker broker = new Broker(env);
		ISyncService server = broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);

		Workspace workspace = workspaceDao.findByName(Constants.WORKSPACEID);
		WorkspaceInfo rWorkspace = new WorkspaceInfo(workspace.getClientWorkspaceName(), workspace.getLatestRevision(), "/");

		long startTotal = System.currentTimeMillis();
		List<ObjectMetadata> response = server.getChanges(Constants.USER, Constants.REQUESTID, rWorkspace);

		System.out.println("Result objects -> " + response.size());
		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");

		System.exit(0);
	}
}
