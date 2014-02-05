package com.stacksync.syncservice.test.benchmark.omq;

import java.util.List;
import java.util.Properties;

import omq.common.broker.Broker;

import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.requests.GetWorkspacesRequest;
import com.stacksync.syncservice.test.benchmark.Constants;

public class TestGetWorkspaces {

	public static void main(String[] args) throws Exception {
		Properties env = RabbitConfig.getProperties();

		Broker broker = new Broker(env);
		ISyncService server = broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);

		long startTotal = System.currentTimeMillis();
		GetWorkspacesRequest request = new GetWorkspacesRequest(Constants.USER);
		List<Workspace> workspaces = server.getWorkspaces(request);

		System.out.println("Result -> " + workspaces);
		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");	
	}
}
