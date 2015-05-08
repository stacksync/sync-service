package com.stacksync.syncservice.startExperimentImpl;

import java.io.FileReader;
import java.sql.Connection;
import java.util.Properties;

import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;
import omq.common.broker.Broker;
import omq.server.RemoteObject;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.dummy.server.ServerDummy2;
import com.stacksync.syncservice.util.Config;

public class StartImpl extends RemoteObject implements Start {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String BROKER_PROPS = "broker.properties";

	private ServerDummy2[] dummies;

	public StartImpl(int numThreads, int numUsers, int commitsPerMinute, int minutes) throws Exception {
		// Load properties
		String configPath = "config.properties";
		Config.loadProperties(configPath);
		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

		// it will try to connect to the DB, throws exception if not
		// possible.
		Connection conn = pool.getConnection();
		conn.close();

		dummies = new ServerDummy2[numThreads];

		for (int i = 0; i < numThreads; i++) {
			// create a new thread
			dummies[i] = new ServerDummy2(pool, numUsers, commitsPerMinute, minutes);
		}

	}

	@Override
	@AsyncMethod
	@MultiMethod
	public void startExperiment() {
		try {
			System.out.println("STARTING EXPERIMENT");

			for (ServerDummy2 dummy : dummies) {
				dummy.start();
			}

			// Wait all the threads
			for (ServerDummy2 dummy : dummies) {
				dummy.join();
				dummy.getConnection().close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param args
	 *            numThreads, numUsers, commitsPerMinute, minutes
	 */
	public static void main(String[] args) {
		int numThreads = Integer.parseInt(args[0]);
		int numUsers = Integer.parseInt(args[1]);
		int commitsPerMinute = Integer.parseInt(args[2]);
		int minutes = Integer.parseInt(args[3]);

		// int numThreads = 4;
		// int numUsers = 1;
		// int commitsPerMinute = 60;
		// int minutes = 1;

		try {
			Properties env = new Properties();
			env.load(new FileReader(BROKER_PROPS));
			Broker broker = new Broker(env);
			broker.bind(BINDING_NAME, new StartImpl(numThreads, numUsers, commitsPerMinute, minutes));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
