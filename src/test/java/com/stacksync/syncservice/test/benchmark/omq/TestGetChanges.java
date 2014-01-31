package com.stacksync.syncservice.test.benchmark.omq;

import java.util.List;
import java.util.Properties;

import omq.common.broker.Broker;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.WorkspaceDAO;
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

		Workspace workspace = workspaceDao.findById(Constants.WORKSPACE_ID);
		User user = new User();
		user.setCloudId(Constants.USER);
		
		long startTotal = System.currentTimeMillis();
		List<ItemMetadata> response = server.getChanges(Constants.REQUEST_ID, user, workspace);

		System.out.println("Result objects -> " + response.size());
		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");

		System.exit(0);
	}
}
