package com.stacksync.syncservice.dummy.server;

import java.sql.Connection;
import java.util.UUID;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;

public class ServerTest {

	/**
	 * 
	 * @param args
	 *            threads, commitsMinute, minutes
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String configPath = "config.properties";
		int numThreads = 4;
		int commitsPerMinute = 60;
		int minutes = 2;

		// TODO set another UUID...
		UUID userID = new UUID(0, 1234);

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
			// // FENT RUN S'EXECUTA LA MERDA AQUESTA EN SECUENCIAL!!!
			// dummy.run();
			dummy.start();
		}

		// Wait all the threads
		for (ServerDummy dummy : dummies) {
			dummy.join();
			dummy.getConnection().close();
		}
		
	}
}
