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
        int numThreads = 1;
        int commitsPerMinute = 60;
        int minutes = 1;

        // TODO set another UUID...
        UUID userID = new UUID(0, 1234);
        UUID deviceID = new UUID(0, 1235);
        UUID workspaceID = new UUID(0, 1236);

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
            dummies[i] = new ServerDummy(pool, userID, commitsPerMinute, minutes);
        }

		// executar a la senyal
        // TODO provar només amb un únic thread
        for (ServerDummy dummy : dummies) {
            dummy.setup(userID, deviceID, workspaceID);
            dummy.start();
        }

        // Wait all the threads
        for (ServerDummy dummy : dummies) {
            dummy.join();
            dummy.getConnection().close();
        }

    }
}
