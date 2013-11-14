package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.postgresql.ds.PGPoolingDataSource;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.DAOConfigurationException;

public class PostgresqlConnectionPool extends ConnectionPool {

	private static final Logger logger = Logger.getLogger(PostgresqlConnectionPool.class.getName());
	private PGPoolingDataSource source;

	public PostgresqlConnectionPool(String host, int port, String database, String username, String password, int initialConns, int maxConns)
			throws DAOConfigurationException {
		try {
			Class.forName("org.postgresql.Driver");

			// Initialize a pooling DataSource
			source = new PGPoolingDataSource();
			source.setDatabaseName(database);
			source.setServerName(host);
			source.setPortNumber(port);
			source.setUser(username);
			source.setPassword(password);
			source.setInitialConnections(initialConns);
			source.setMaxConnections(maxConns);
			source.getConnection().close();

		} catch (ClassNotFoundException e) {
			logger.error("Where is your PostgreSQL JDBC Driver? " + "Include in your library path!");
			throw new DAOConfigurationException("PostgreSQL JDBC driver not found");
		} catch (SQLException e) {
			logger.error("SQLException catched at DAOFactory", e);
		}

	}

	@Override
	public synchronized Connection getConnection() throws SQLException {
		try {
			return source.getConnection();
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
}
