package com.stacksync.syncservice.rmiserver;

//import java.rmi.Naming;
import java.rmi.RemoteException;
//import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//import org.apache.log4j.Logger;

//import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;

//import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;

public class PostgresqlUserDAORMISer extends UnicastRemoteObject implements
		UserDAORMISer {
	// private static final Logger logger = Logger
	// .getLogger(PostgresqlUserDAO.class.getName());

	public PostgresqlUserDAORMISer() throws RemoteException {
	}

	@Override
	public UserRMI findById(UUID userID) throws DAOException {
		// ResultSet resultSet = null;
		UserRMI user = new UserRMI();
		user.setName("Laura");
		user.setId(UUID.randomUUID());
		user.setEmail("laura@jo.com");
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		/*
		 * String query =
		 * "SELECT id, name, email, swift_user, swift_account, quota_limit, quota_used "
		 * + " FROM \"user1\" WHERE id = ?::uuid";
		 * 
		 * try { resultSet = executeQuery(query, new Object[] { userID });
		 * 
		 * if (resultSet.next()) { user = mapUser(resultSet); } } catch
		 * (SQLException e) { logger.error(e); throw new
		 * DAOException(DAOError.INTERNAL_SERVER_ERROR); }
		 * 
		 * if (user == null) { throw new DAOException(DAOError.USER_NOT_FOUND);
		 * }
		 */

		return user;
	}

	@Override
	public UserRMI getByEmail(String email) throws DAOException {

		// ResultSet resultSet = null;
		UserRMI user = null;

		/*
		 * String query = "SELECT * " +
		 * " FROM \"user1\" WHERE email = lower(?)";
		 * 
		 * try { resultSet = executeQuery(query, new Object[] { email });
		 * 
		 * if (resultSet.next()) { user = mapUser(resultSet); } else { throw new
		 * NoResultReturnedDAOException(DAOError.USER_NOT_FOUND); } } catch
		 * (SQLException e) { throw new
		 * DAOException(DAOError.INTERNAL_SERVER_ERROR); }
		 */

		return user;
	}

	@Override
	public List<UserRMI> findAll() throws DAOException {

		// ResultSet resultSet = null;
		List<UserRMI> list = new ArrayList<UserRMI>();

		/*
		 * String query = "SELECT * FROM user1"; try { resultSet =
		 * executeQuery(query, null);
		 * 
		 * while (resultSet.next()) { list.add(mapUser(resultSet)); } } catch
		 * (SQLException e) { logger.error(e); throw new
		 * DAOException(DAOError.INTERNAL_SERVER_ERROR); }
		 */
		return list;
	}

	@Override
	public void add(UserRMI user) throws DAOException {
		/*
		 * if (!user.isValid()) { throw new
		 * IllegalArgumentException("User attributes not set"); }
		 * 
		 * Object[] values = { user.getEmail(), user.getName(),
		 * user.getSwiftUser(), user.getSwiftAccount(), user.getQuotaLimit(),
		 * user.getQuotaUsed() };
		 * 
		 * String query =
		 * "INSERT INTO user1 (email, name, swift_user, swift_account, quota_limit, quota_used) VALUES (?, ?, ?, ?, ?)"
		 * ;
		 * 
		 * try { UUID userId = (UUID) executeUpdate(query, values);
		 * user.setId(userId); } catch (DAOException e) { logger.error(e); throw
		 * new DAOException(DAOError.INTERNAL_SERVER_ERROR); }
		 */
	}

	@Override
	public void update(UserRMI user) throws DAOException {
		/*
		 * if (user.getId() == null || !user.isValid()) { throw new
		 * IllegalArgumentException("User attributes not set"); }
		 * 
		 * Object[] values = { user.getEmail(), user.getName(),
		 * user.getSwiftUser(), user.getSwiftAccount(), user.getQuotaLimit(),
		 * user.getQuotaUsed(), user.getId() };
		 * 
		 * String query =
		 * "UPDATE user1 SET email = ?, name = ?, swift_user = ?, swift_account = ?, quota_limit = ?, quota_used = ? WHERE id = ?::uuid"
		 * ;
		 * 
		 * try { executeUpdate(query, values); } catch (DAOException e) {
		 * logger.error(e); throw new DAOException(e); }
		 */
	}

	@Override
	public void delete(UUID userID) throws DAOException {
		/*
		 * Object[] values = { userID };
		 * 
		 * String query = "DELETE FROM user1 WHERE id = ?";
		 * 
		 * executeUpdate(query, values);
		 */
	}

	/*
	 * private User mapUser(ResultSet resultSet) throws SQLException { User user
	 * = new User(); user.setId(UUID.fromString(resultSet.getString("id")));
	 * user.setEmail(resultSet.getString("email"));
	 * user.setName(resultSet.getString("name"));
	 * user.setSwiftUser(resultSet.getString("swift_user"));
	 * user.setSwiftAccount(resultSet.getString("swift_account"));
	 * user.setQuotaLimit(resultSet.getInt("quota_limit"));
	 * user.setQuotaUsed(resultSet.getInt("quota_used")); return user; }
	 */
	@Override
	public List<UserRMI> findByItemId(Long itemId) throws DAOException {
		ArrayList<UserRMI> users = new ArrayList<UserRMI>();
		/*
		 * Object[] values = { itemId };
		 * 
		 * String query = "SELECT u.* " + " FROM item i " +
		 * " INNER JOIN workspace_user wu ON i.workspace_id = wu.workspace_id "
		 * + " INNER JOIN user1 u ON wu.user_id = u.id " + " WHERE i.id = ?";
		 * 
		 * ResultSet result = null;
		 * 
		 * try { result = executeQuery(query, values);
		 * 
		 * while (result.next()) { User user = mapUser(result); users.add(user);
		 * }
		 * 
		 * } catch (SQLException e) { logger.error(e); throw new
		 * DAOException(DAOError.INTERNAL_SERVER_ERROR); }
		 */

		return users;
	}

}
