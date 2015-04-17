package com.stacksync.syncservice.db.postgresql;


import java.sql.Connection;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.UserExternalDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;

public class PostgresqlUserExternalDAO extends PostgresqlDAO implements UserExternalDAO {
	private static final Logger logger = Logger.getLogger(PostgresqlUserDAO.class.getName());

	public PostgresqlUserExternalDAO(Connection connection) {
		super(connection);
	}

	@Override
	public void add(User user) throws DAOException {

		Object[] values = { user.getId(), user.getEmail(), user.getName(), user.getSwiftUser()};

		String query = "INSERT INTO cloudspaces_user (key, email, name, swift_user, now(), now()) VALUES (?, ?, ?, ?)";

		try {
			Long id = (Long) executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
	}

}
