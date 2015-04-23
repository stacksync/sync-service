package com.stacksync.syncservice.db.postgresql;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
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

		Object[] values = { user.getId(), user.getName(), user.getSwiftUser(), user.getEmail()};

		String query = "INSERT INTO cloudspaces_user (key, name, swift_name, email, updated_at, created_at) VALUES (?::uuid, ?, ?, ?, now(), now())";

		try {
			Integer id = (Integer) executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public User getByEmail(String email) throws DAOException {

		ResultSet resultSet = null;
		User user = null;

		String query = "SELECT * " + " FROM \"cloudspaces_user\" WHERE email = lower(?)";

		try {
			resultSet = executeQuery(query, new Object[] { email });
			
			if (resultSet.next()) {
				user = mapUser(resultSet);
			}else{
				throw new NoResultReturnedDAOException(DAOError.USER_NOT_FOUND);
			}
		} catch (SQLException e) {
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return user;
	}
	
	private User mapUser(ResultSet resultSet) throws SQLException {
		User user = new User();
		user.setId(UUID.fromString(resultSet.getString("key")));
		user.setEmail(resultSet.getString("email"));
		user.setName(resultSet.getString("name"));
		user.setSwiftUser(resultSet.getString("swift_name"));
		user.setQuotaUsedReal(0L);
		user.setQuotaLimit(0L);
		user.setQuotaUsedLogical(0L);
		return user;
	}

}
