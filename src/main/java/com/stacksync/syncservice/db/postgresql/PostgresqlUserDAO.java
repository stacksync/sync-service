package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class PostgresqlUserDAO extends PostgresqlDAO implements UserDAO {
	private static final Logger logger = Logger.getLogger(PostgresqlUserDAO.class.getName());

	public PostgresqlUserDAO(Connection connection) {
		super(connection);
	}

	@Override
	public User findByPrimaryKey(Long userID) throws DAOException {
		ResultSet resultSet = null;
		User user = null;

		String query = "SELECT id, name, email, cloud_id, quota_limit, quota_used " + " FROM \"user1\" WHERE id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { userID });

			if (resultSet.next()) {
				user = mapUser(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return user;
	}

	@Override
	public User findByCloudId(String cloudID) throws DAOException {

		ResultSet resultSet = null;
		User user = null;

		String query = "SELECT id, name, email, cloud_id, quota_limit, quota_used " + " FROM \"user1\" WHERE cloud_id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { cloudID });

			if (resultSet.next()) {
				user = mapUser(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return user;
	}
	
	@Override
	public User findByEmail(String email) throws DAOException {

		ResultSet resultSet = null;
		User user = null;

		String query = "SELECT id, name, email, cloud_id, quota_limit, quota_used " + " FROM \"user1\" WHERE email = ?";

		try {
			resultSet = executeQuery(query, new Object[] { email });

			if (resultSet.next()) {
				user = mapUser(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return user;
	}

	@Override
	public List<User> findAll() throws DAOException {

		ResultSet resultSet = null;
		List<User> list = new ArrayList<User>();

		String query = "SELECT * FROM user1";
		try {
			resultSet = executeQuery(query, null);

			while (resultSet.next()) {
				list.add(mapUser(resultSet));
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
		return list;
	}

	@Override
	public void add(User user) throws DAOException {
		if (!user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}

		Object[] values = { user.getEmail(), user.getName(), user.getCloudId(), user.getQuotaLimit(), user.getQuotaUsed() };

		String query = "INSERT INTO user1 (email, name, cloud_id, quota_limit, quota_used) VALUES (?, ?, ?, ?, ?)";

		try {
			long userId = executeUpdate(query, values);
			user.setId(userId);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void update(User user) throws DAOException {
		if (user.getId() == null || !user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}

		Object[] values = { user.getEmail(), user.getName(), user.getCloudId(), user.getQuotaLimit(), user.getQuotaUsed(), user.getId() };

		String query = "UPDATE user1 SET email = ?, name = ?, cloud_id = ?, quota_limit = ?, quota_used = ? WHERE id = ?";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(e);
		}
	}

	@Override
	public void delete(Long userID) throws DAOException {
		Object[] values = { userID };

		String query = "DELETE FROM user1 WHERE id = ?";

		executeUpdate(query, values);
	}

	@Override
	public void delete(String cloudID) throws DAOException {

		Object[] values = { cloudID };

		String query = "DELETE FROM user1 WHERE cloud_id = ?";

		executeUpdate(query, values);
	}

	private User mapUser(ResultSet resultSet) throws SQLException {
		User user = new User();
		user.setId(resultSet.getLong("id"));
		user.setEmail(resultSet.getString("email"));
		user.setName(resultSet.getString("name"));
		user.setCloudId(resultSet.getString("cloud_id"));
		user.setQuotaLimit(resultSet.getInt("quota_limit"));
		user.setQuotaUsed(resultSet.getInt("quota_used"));
		return user;
	}

	@Override
	public List<User> findByItemId(Long itemId) throws DAOException {
		ArrayList<User> users = new ArrayList<User>();
		Object[] values = { itemId };

		String query = "SELECT u.* " + " FROM item i " + " INNER JOIN workspace_user wu ON i.workspace_id = wu.workspace_id "
				+ " INNER JOIN user1 u ON wu.user_id = u.id " + " WHERE o.id = ?";

		ResultSet result = null;

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				User user = mapUser(result);
				users.add(user);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return users;
	}

}
