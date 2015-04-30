/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class CommitsPerSecondTest {
   
    /**
     *
     * @param args comitsSec, numUsers, minutes, threads
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String configPath = "config.properties";

        if (args.length != 4) {

            System.err.println("Usage: commitsPerSecond numUsers minutes threads");
            System.exit(0);
        }

        int commitsPerSecond = Integer.parseInt(args[0]);
        int numUsers = Integer.parseInt(args[1]);
        int minutes = Integer.parseInt(args[2]);
        int numThreads = Integer.parseInt(args[3]);;

        // Load properties
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

        // it will try to connect to the DB, throws exception if not
        // possible.
        Connection conn = pool.getConnection();
        conn.close();

        StaticBenchmark[] dummies = new StaticBenchmark[numThreads];

        for (int i = 0; i < numThreads; i++) {
            // Crear un nou thread
            dummies[i] = new StaticBenchmark(pool, numUsers, commitsPerSecond, minutes);
        }

        // executar a la senyal
        // TODO provar només amb un únic thread
        for (int i = 0; i < numThreads; i++) {
            StaticBenchmark dummy = dummies[i];
            dummy.start();
        }

        int numLines = 0;
        // Wait all the threads
        for (StaticBenchmark dummy : dummies) {
            dummy.join();
            dummy.getConnection().close();
            numLines = dummy.getItemsCount();
        }

        System.out.println("END - Commits made: " + numThreads * numLines);

        System.exit(0);
    }
}
