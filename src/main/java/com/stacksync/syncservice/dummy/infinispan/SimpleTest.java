/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class SimpleTest {
    protected final Logger logger = Logger.getLogger(CommitsPerSecondTest.class.getName());
    private StaticBenchmark[] threads;
    private int commitsPerSecond, minutes, numUsers, numThreads;
    private ConnectionPool pool;

    public SimpleTest(int numThreads, int numUsers, int commitsPerSecond, int minutes) throws Exception {

        this.commitsPerSecond = commitsPerSecond;
        this.minutes = minutes;
        this.numUsers = numUsers;
        this.numThreads = numThreads;

        // Load properties
        String configPath = "config.properties";
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        this.pool = ConnectionPoolFactory.getConnectionPool(datasource);
        
        this.threads = this.initializeThreads(numThreads, numUsers, commitsPerSecond, minutes);
    }

    private StaticBenchmark[] initializeThreads(int numThreads, int numUsers, int commitsPerSecond, int minutes) throws Exception {
        StaticBenchmark[] benchmarkThreads = new StaticBenchmark[numThreads];
        for (int i = 0; i < numThreads; i++) {
            // create a new thread
            benchmarkThreads[i] = new StaticBenchmark(this.pool, numUsers, commitsPerSecond, minutes);
        }
        return benchmarkThreads;
    }

    public void startExperiment() {
        try {
            logger.info("STARTING EXPERIMENT");

            for (StaticBenchmark thread : threads) {
                thread.start();
            }

            // Wait all the threads
            for (StaticBenchmark thread : threads) {
                thread.join();
                thread.getConnection().close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void setThreads(StaticBenchmark[] threads) {
        this.threads = threads;
    }

    /**
     *
     * @param args comitsSec, numUsers, minutes, threads
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 4) {

            System.err.println("Usage: commitsPerSecond numUsers minutes threads");
            System.exit(0);
        }

        int commitsPerSecond = Integer.parseInt(args[0]);
        int numUsers = Integer.parseInt(args[1]);
        int minutes = Integer.parseInt(args[2]);
        int numThreads = Integer.parseInt(args[3]);
        
        SimpleTest test = new SimpleTest(numThreads, numUsers, commitsPerSecond, minutes);
        test.startExperiment();
    }
}
