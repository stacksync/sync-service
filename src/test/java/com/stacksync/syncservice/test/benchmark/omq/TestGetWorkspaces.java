package com.stacksync.syncservice.test.benchmark.omq;

import java.util.List;
import java.util.Properties;

import omq.common.broker.Broker;

import com.stacksync.syncservice.models.WorkspaceInfo;
import com.stacksync.syncservice.omq.ISyncService;
import com.stacksync.syncservice.test.benchmark.Constants;

public class TestGetWorkspaces {

	public static void main(String[] args) throws Exception {
		Properties env = RabbitConfig.getProperties();

		Broker broker = new Broker(env);
		ISyncService server = broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);

		long startTotal = System.currentTimeMillis();
		List<WorkspaceInfo> workspaces = server.getWorkspaces(Constants.USER, Constants.REQUESTID);

		System.out.println("Result -> " + workspaces);
		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");	
	}
}
