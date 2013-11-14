package com.stacksync.syncservice.test.benchmark.omq;

import java.util.Properties;

import omq.common.util.ParameterQueue;

public class RabbitConfig {

	public static Properties getProperties() {
		Properties env = new Properties();
		env.setProperty(ParameterQueue.USER_NAME, "guest");
		env.setProperty(ParameterQueue.USER_PASS, "guest");

		// Get host info of rabbimq (where it is)
		env.setProperty(ParameterQueue.RABBIT_HOST, "10.30.239.228");
		env.setProperty(ParameterQueue.RABBIT_PORT, "5672");

		// Get info about the queue & the exchange where the RemoteListener will
		// listen to.
		env.setProperty(ParameterQueue.RPC_EXCHANGE, "rpc_global_exchange");

		// Set info about the queue & the exchange where the ResponseListener
		// will listen to.
		env.setProperty(ParameterQueue.RPC_REPLY_QUEUE, "reply_gguerrero201305141718");

		return env;
	}
}
