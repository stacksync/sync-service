/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.Connection;
import java.util.UUID;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;

public class DummyServerTest {

    /**
     *
     * @param args commitsMinute, numUsers, minutes
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String configPath = "config.properties";

        if (args.length != 3) {

            System.err.println("Usage: commitsPerMinute numUsers minutes");
            System.exit(0);
        }

        int commitsPerMinute = Integer.parseInt(args[0]);
        int numUsers = Integer.parseInt(args[1]);
        int minutes = Integer.parseInt(args[2]);
        int numThreads = 2;

        // Load properties
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

        // it will try to connect to the DB, throws exception if not
        // possible.
        Connection conn = pool.getConnection();
        conn.close();

        ServerDummy[] dummies = new ServerDummy[numThreads];

        for (int i = 0; i < numThreads; i++) {
            // Crear un nou thread
            dummies[i] = new ServerDummy(pool, numUsers, commitsPerMinute, minutes);
        }

        // executar a la senyal
        // TODO provar només amb un únic thread
        for (int i = 0; i < numThreads; i++) {
            ServerDummy dummy = dummies[i];
            dummy.start();
        }

        // Wait all the threads
        for (ServerDummy dummy : dummies) {
            dummy.join();
            dummy.getConnection().close();
        }

        System.out.println("END - Commits made: " + minutes * commitsPerMinute * numThreads);

        System.exit(0);
    }
}
