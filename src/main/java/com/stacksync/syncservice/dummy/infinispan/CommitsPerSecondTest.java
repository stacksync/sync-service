/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;
import java.io.FileReader;
import java.util.Properties;
import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;
import omq.common.broker.Broker;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class CommitsPerSecondTest extends RemoteObject implements Start {

    protected final Logger logger = Logger.getLogger(CommitsPerSecondTest.class.getName());
    private static final String BROKER_PROPS = "broker.properties";
    private StaticBenchmark[] threads;
    private int commitsPerSecond, minutes, numUsers, numThreads;
    private Broker broker;
    private ConnectionPool pool;

    public CommitsPerSecondTest(int numThreads, int numUsers, int commitsPerSecond, int minutes) throws Exception {

        this.commitsPerSecond = commitsPerSecond;
        this.minutes = minutes;
        this.numUsers = numUsers;
        this.numThreads = numThreads;

        // Load properties
        String configPath = "config.properties";
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        this.pool = ConnectionPoolFactory.getConnectionPool(datasource);

        Properties env = new Properties();
        env.load(new FileReader(BROKER_PROPS));
        this.broker = new Broker(env);
        
        System.out.println("Ready to g0!");
    }

    private StaticBenchmark[] initializeThreads(int numThreads, int numUsers, int commitsPerSecond, int minutes) throws Exception {
        StaticBenchmark[] benchmarkThreads = new StaticBenchmark[numThreads];
        for (int i = 0; i < numThreads; i++) {
            // create a new thread
            benchmarkThreads[i] = new StaticBenchmark(this.pool, numUsers, commitsPerSecond, minutes);
        }
        return benchmarkThreads;
    }

    @Override
    @AsyncMethod
    @MultiMethod
    public void startWarmUp(int numThreads, int numUsers, int commitsPerSecond, int minutes) {
        
        logger.info("Start warm up!!");
        try {
            this.threads = initializeThreads(numThreads, numUsers, commitsPerSecond, minutes);
            for (StaticBenchmark thread : threads) {
                thread.start();
            }

            // Wait all the threads
            for (StaticBenchmark thread : threads) {
                thread.join();
                thread.getConnection().close();
            }

            // It is necessary to reinitialize threads after the warm up
            this.threads = initializeThreads(this.numThreads, this.numUsers, this.commitsPerSecond, this.minutes);

            Notifier notifier = this.broker.lookup(Notifier.BINDING_NAME, Notifier.class);
            notifier.endWarmUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @AsyncMethod
    @MultiMethod
    public void startExperiment() {
        try {
            logger.info("STARTING EXPERIMENT");

            for (StaticBenchmark thread : threads) {
                //thread.setCommitsPerSecond(this.commitsPerSecond);
                //thread.setMinutes(this.minutes);
                thread.start();
            }

            // Wait all the threads
            for (StaticBenchmark thread : threads) {
                thread.join();
                thread.getConnection().close();
            }

            Notifier notifier = this.broker.lookup(Notifier.BINDING_NAME, Notifier.class);
            notifier.endExperiment();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param args comitsSec, numUsers, minutes, threads
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        /*if (args.length != 4) {

            System.err.println("Usage: commitsPerSecond numUsers minutes threads");
            System.exit(0);
        }

        int commitsPerSecond = Integer.parseInt(args[0]);
        int numUsers = Integer.parseInt(args[1]);
        int minutes = Integer.parseInt(args[2]);
        int numThreads = Integer.parseInt(args[3]);*/

        int commitsPerSecond = 1;
        int numUsers = 1;
        int minutes = 1;
        int numThreads = 1;
        
        try {
            Properties env = new Properties();
            env.load(new FileReader(BROKER_PROPS));
            Broker broker = new Broker(env);
            broker.bind(BINDING_NAME, new CommitsPerSecondTest(numThreads, numUsers, commitsPerSecond, minutes));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
