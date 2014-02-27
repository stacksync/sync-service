package com.stacksync.syncservice.test.benchmark.normal;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.util.Config;

public class TestGetWorkspaces {

	public static void main(String[] args) throws Exception {

		Config.loadProperties();
		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		Handler handler = new SQLHandler(pool);

		long startTotal = System.currentTimeMillis();

		User user = new User(Constants.USER);
		
		handler.doGetWorkspaces(user);

		/*
		 * List<RemoteWorkspace> list = new ArrayList<RemoteWorkspace>(); for
		 * (Workspace w : response.getWorkspaces()) { try { RemoteWorkspace
		 * workspace = new RemoteWorkspaceImp(w); list.add(workspace); } catch
		 * (Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } }
		 */

		long totalTime = System.currentTimeMillis() - startTotal;
		// System.out.println("Result -> " + list);
		System.out.println("Total level time --> " + totalTime + " ms");
	}
}
