package com.stacksync.syncservice.db.postgresql;

import static com.stacksync.syncservice.db.DAOUtil.prepareStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoRowsAffectedDAOException;

public class PostgresqlDAO {
	private static final Logger logger = Logger.getLogger(PostgresqlDAO.class.getName());
	protected Connection connection;

	public PostgresqlDAO(Connection connection) {
		this.connection = connection;
	}

	protected ResultSet executeQuery(String query, Object[] values) throws DAOException {

		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			preparedStatement = prepareStatement(connection, query, false, values);
			resultSet = preparedStatement.executeQuery();

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(e, DAOError.INTERNAL_SERVER_ERROR);
		}

		return resultSet;
	}

	protected Object executeUpdate(String query, Object[] values) throws DAOException {

		Object key = null;
		PreparedStatement preparedStatement = null;
		ResultSet generatedKeys = null;

		try {
			preparedStatement = prepareStatement(connection, query, true, values);
			int affectedRows = preparedStatement.executeUpdate();
			if (affectedRows == 0) {
				throw new NoRowsAffectedDAOException("Execute update error: no rows affected.");
			}

			if (query.startsWith("INSERT")) {
				generatedKeys = preparedStatement.getGeneratedKeys();

				if (generatedKeys.next()) {
					key = generatedKeys.getObject(1);
				} else {
					throw new DAOException("Creating object failed, no generated key obtained.");
				}
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(e, DAOError.INTERNAL_SERVER_ERROR);
		}

		return key;
	}
}
