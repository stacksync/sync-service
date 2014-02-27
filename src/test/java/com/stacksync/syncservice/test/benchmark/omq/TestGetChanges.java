package com.stacksync.syncservice.test.benchmark.omq;

import java.util.List;
import java.util.Properties;

import omq.common.broker.Broker;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.requests.GetChangesRequest;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.util.Config;

public class TestGetChanges {

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		Properties env = RabbitConfig.getProperties();

		Broker broker = new Broker(env);
		ISyncService server = broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);
		
		long startTotal = System.currentTimeMillis();
		
		GetChangesRequest request = new GetChangesRequest(Constants.USER, Constants.WORKSPACE_ID);
		
		List<ItemMetadata> response = server.getChanges(request);

		System.out.println("Result objects -> " + response.size());
		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");

		System.exit(0);
	}
}
