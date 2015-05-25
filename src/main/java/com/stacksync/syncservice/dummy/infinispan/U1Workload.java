/**
 *
 */
package com.stacksync.syncservice.dummy.infinispan;

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
public class U1Workload extends RemoteObject implements Start {

    private static final String BROKER_PROPS = "broker.properties";
    private static final String CONFIG_PATH = "config.properties";
    private static final long serialVersionUID = 1L;
    private final Logger logger = Logger.getLogger(U1Workload.class.getName());
    
    private List<U1BenchmarkWorker> threads;
    private ConnectionPool pool;

    public U1Workload(String configPath, String folderPath) throws IOException, DAOConfigurationException {
        // Load connection pool
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        pool = ConnectionPoolFactory.getConnectionPool(datasource);

        // Create threads
        threads = new ArrayList<U1BenchmarkWorker>();
        File folder = new File(folderPath);

        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                try {
                    threads.add(new U1BenchmarkWorker(pool, file));
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }

    }

    @Override
    public void startWarmUp(int numThreads, int numUsers, int commitsPerSecond, int minutes) {
        return;
    }

    @Override
    public void startExperiment() {

        for (U1BenchmarkWorker thread : threads) {
            thread.start();
        }

        for (U1BenchmarkWorker thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: folder_path");
            System.exit(0);
        }

        try {
            Properties env = new Properties();
            env.load(new FileReader(BROKER_PROPS));
            Broker broker = new Broker(env);
            broker.bind(BINDING_NAME, new U1Workload(CONFIG_PATH, args[0]));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
