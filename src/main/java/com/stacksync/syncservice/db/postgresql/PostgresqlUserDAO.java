package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;

public class PostgresqlUserDAO extends PostgresqlDAO implements UserDAO {
	private static final Logger logger = Logger.getLogger(PostgresqlUserDAO.class.getName());

	public PostgresqlUserDAO(Connection connection) {
		super(connection);
	}

	@Override
	public User findById(UUID userID) throws DAOException {
		ResultSet resultSet = null;
		User user = null;

		String query = "SELECT * FROM find_user_by_id(?::uuid)";

		try {
			resultSet = executeQuery(query, new Object[] { userID });

			if (resultSet.next()) {
				user = mapUser(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		if (user == null) {
			throw new DAOException(DAOError.USER_NOT_FOUND);
		}

		return user;
	}

	@Override
	public User getByEmail(String email) throws DAOException {

		ResultSet resultSet = null;
		User user = null;

		String query = "SELECT * FROM get_user_by_email(lower(?))";

		try {
			resultSet = executeQuery(query, new Object[] { email });

			if (resultSet.next()) {
				user = mapUser(resultSet);
			} else {
				throw new NoResultReturnedDAOException(DAOError.USER_NOT_FOUND);
			}
		} catch (SQLException e) {
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return user;
	}

	@Override
	public List<User> findAll() throws DAOException {

		ResultSet resultSet = null;
		List<User> list = new ArrayList<User>();

		String query = "SELECT * FROM find_all_users()";
		try {
			resultSet = executeQuery(query, new Object[] {});

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
		// This will insert a new user in one of the shards:
		// "RUN ON hashtext(id::text);"
		UUID userID = UUID.randomUUID();

		Object[] values = { userID, user.getEmail(), user.getName(), user.getSwiftUser(), user.getSwiftAccount(), user.getQuotaLimit(),
				user.getQuotaUsed() };

		for (Object o : values) {
			if (o == null) {
				throw new IllegalArgumentException("User attributes not set");
			}
		}

		String query = "SELECT add_user(?::uuid, ?, ?, ?, ?, ?, ?)";

		try {
			executeQuery(query, values);
			user.setId(userID);
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

		Object[] values = { user.getId(), user.getEmail(), user.getName(), user.getSwiftUser(), user.getSwiftAccount(),
				user.getQuotaLimit(), user.getQuotaUsed() };

		String query = "SELECT update_user(?::uuid, ?, ?, ?, ?, ?, ?)";

		try {
			executeQuery(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(e);
		}
	}

	@Override
	public void delete(UUID userID) throws DAOException {
		Object[] values = { userID };

		String query = "SELECT delete_user(?::uuid)";

		executeQuery(query, values);
	}

	private User mapUser(ResultSet resultSet) throws SQLException {
		User user = new User();
		user.setId(UUID.fromString(resultSet.getString("id")));
		user.setEmail(resultSet.getString("email"));
		user.setName(resultSet.getString("name"));
		user.setSwiftUser(resultSet.getString("swift_user"));
		user.setSwiftAccount(resultSet.getString("swift_account"));
		user.setQuotaLimit(resultSet.getInt("quota_limit"));
		user.setQuotaUsed(resultSet.getInt("quota_used"));
		return user;
	}

	@Override
	public List<User> findByItemId(Long itemId) throws DAOException {
		ArrayList<User> users = new ArrayList<User>();
		Object[] values = { itemId };

		String query = "SELECT * FROM find_user_by_item_id(?)";

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
