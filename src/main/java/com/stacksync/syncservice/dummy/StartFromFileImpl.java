/**
 * 
 */
package com.stacksync.syncservice.dummy;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import omq.common.broker.Broker;
import omq.server.RemoteObject;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.util.Config;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class StartFromFileImpl extends RemoteObject implements StartFromFile {
	private static final String BROKER_PROPS = "broker.properties";
	private static final String CONFIG_PATH = "config.properties";
	private static final String FOLDER_PATH = "u1_commits";

	private static final long serialVersionUID = 1L;

	private final Logger logger = Logger.getLogger(StartFromFileImpl.class.getName());

	private List<WorkloadFromFileBenchmarkThread> threads;
	private ConnectionPool pool;

	public StartFromFileImpl(String configPath, String folderPath) throws IOException, DAOConfigurationException {
		// Load connection pool
		Config.loadProperties(configPath);
		String datasource = Config.getDatasource();
		pool = ConnectionPoolFactory.getConnectionPool(datasource);

		// Create threads
		threads = new ArrayList<WorkloadFromFileBenchmarkThread>();
		File folder = new File(folderPath);

		for (File file : folder.listFiles()) {
			if (file.isFile()) {
				try {
					threads.add(new WorkloadFromFileBenchmarkThread(pool, file));
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}

	}

	@Override
	public void startExperiment() {

		for (WorkloadFromFileBenchmarkThread thread : threads) {
			thread.start();
		}

		for (WorkloadFromFileBenchmarkThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}

	public static void main(String[] args) {

		try {
			Properties env = new Properties();
			env.load(new FileReader(BROKER_PROPS));
			Broker broker = new Broker(env);
			broker.bind(BINDING_NAME, new StartFromFileImpl(CONFIG_PATH, FOLDER_PATH));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
