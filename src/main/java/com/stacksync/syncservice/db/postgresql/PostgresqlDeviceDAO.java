package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Constants;

public class PostgresqlDeviceDAO extends PostgresqlDAO implements DeviceDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlDeviceDAO.class.getName());

	public PostgresqlDeviceDAO(Connection connection) {
		super(connection);
	}

	@Override
	public Device get(UUID userID, UUID deviceID) throws DAOException {

		// API device ID is not stored in the database
		if (deviceID == Constants.API_DEVICE_ID) {
			return new Device(Constants.API_DEVICE_ID);
		}

		ResultSet resultSet = null;
		Device device = null;

		String query = "SELECT * FROM get_item_by_id(?::uuid, ?)";

		try {
			resultSet = executeQuery(query, new Object[] { userID, deviceID });

			if (resultSet.next()) {
				device = mapDevice(resultSet);
			}
		} catch (SQLException e) {
			throw new DAOException(e);
		}

		return device;
	}

	@Override
	public void add(Device device) throws DAOException {
		if (!device.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}

		Object[] values = { device.getUser().getId(), device.getName(), device.getOs(), device.getLastIp(), device.getAppVersion() };

		String query = "SELECT add_item(?::uuid, ?, ?, ?)";

		ResultSet resultSet = executeQuery(query, values);

		UUID id;

		try {
			if (resultSet.next()) {
				id = (UUID) resultSet.getObject(1);
				if (id != null) {
					device.setId(id);
				}
			} else {
				throw new DAOException("Creating object failed, no generated key obtained.");
			}
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	@Override
	public void update(Device device) throws DAOException {
		if (device.getId() == null || !device.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}

		Object[] values = { device.getUser().getId(), device.getId(), device.getLastIp(), device.getAppVersion() };

		String query = "SELECT update_item(?::uuid, ?::uuid, ?, ?)";

		try {
			executeQuery(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(e);
		}
	}

	@Override
	public void delete(UUID userID, UUID deviceID) throws DAOException {
		Object[] values = { userID, deviceID };

		String query = "SELECT delete_device(?::uuid, ?::uuid)";

		executeQuery(query, values);
	}

	private Device mapDevice(ResultSet resultSet) throws SQLException {

		Device device = new Device();
		device.setId(UUID.fromString(resultSet.getString("id")));
		device.setName(resultSet.getString("name"));

		User user = new User();
		user.setId(UUID.fromString(resultSet.getString("user_id")));

		device.setUser(user);

		return device;

	}

}
