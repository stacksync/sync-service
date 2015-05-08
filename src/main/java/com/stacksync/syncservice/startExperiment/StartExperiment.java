/**
 * 
 */
package com.stacksync.syncservice.startExperiment;

import java.io.FileReader;
import java.util.Properties;

import omq.common.broker.Broker;

import com.stacksync.syncservice.startExperimentImpl.Start;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class StartExperiment {
	private static final String BROKER_PROPS = "broker.properties";

	public static void main(String[] args) throws Exception {
		Properties env = new Properties();
		env.load(new FileReader(BROKER_PROPS));
		Broker broker = new Broker(env);

		Start startExperiment = broker.lookup(Start.BINDING_NAME, Start.class);

		startExperiment.startExperiment();

		Thread.sleep(1000);

		broker.stopBroker();

	}
}
