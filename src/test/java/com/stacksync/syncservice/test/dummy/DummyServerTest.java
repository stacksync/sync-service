/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.test.dummy;

import com.stacksync.syncservice.db.Connection;
import java.util.UUID;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;

public class DummyServerTest {

    /**
     *
     * @param args threads, commitsMinute, minutes
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String configPath = "config.properties";
        int numThreads = 2;
        int commitsPerMinute = 60;
        int numUsers = 2;
        int minutes = 1;

        // Load properties
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

        // it will try to connect to the DB, throws exception if not
        // possible.
        Connection conn = pool.getConnection();
        conn.close();

        ServerDummy2[] dummies = new ServerDummy2[numThreads];
        UUID[] uuids = new UUID[numThreads];

        for (int i = 0; i < numThreads; i++) {
            // Crear un nou thread
            uuids[i] = UUID.randomUUID();
            dummies[i] = new ServerDummy2(pool, numUsers, commitsPerMinute, minutes);
        }

        // executar a la senyal
        // TODO provar només amb un únic thread
        for (int i = 0; i < numThreads; i++) {
            ServerDummy2 dummy = dummies[i];
            dummy.setup(uuids[i]);
            dummy.start();
        }

        // Wait all the threads
        for (ServerDummy2 dummy : dummies) {
            dummy.join();
            dummy.getConnection().close();
        }

        System.out.println("END - Commits made: " + minutes * commitsPerMinute);

    }
}
