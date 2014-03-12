package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.ds.PGPoolingDataSource;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;

public class PostgresqlConnectionPool extends ConnectionPool {

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
			throw new DAOConfigurationException("PostgreSQL JDBC driver not found", e);
		} catch (SQLException e) {
			throw new DAOConfigurationException("SQLException catched at DAOFactory", e);
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
