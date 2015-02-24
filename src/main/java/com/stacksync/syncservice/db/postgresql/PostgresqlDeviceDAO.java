package com.stacksync.syncservice.db.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Constants;

public class PostgresqlDeviceDAO extends PostgresqlDAO implements DeviceDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlDeviceDAO.class.getName());

	public PostgresqlDeviceDAO(Connection connection) {
		super((PostgresqlConnection)connection);
	}

	@Override
	public Device get(UUID deviceID) throws DAOException {
		
		// API device ID is not stored in the database
		if(deviceID == Constants.API_DEVICE_ID){
			return new Device(Constants.API_DEVICE_ID);
		}
		
		ResultSet resultSet = null;
		Device device = null;

		String query = "SELECT * FROM device WHERE id = ?::uuid";

		try {
			resultSet = executeQuery(query, new Object[] { deviceID });

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

		Object[] values = { device.getName(), device.getUser().getId(), device.getOs(), 
				device.getLastIp(), device.getAppVersion() };

		String query = "INSERT INTO device (name, user_id, os, created_at, last_access_at, last_ip, app_version) "
				+ "VALUES (?, ?::uuid, ?, now(), now(), ?::inet, ?)";

		UUID id = (UUID) executeUpdate(query, values);

		if (id != null) {
			device.setId(id);
		}
	}

	@Override
	public void update(Device device) throws DAOException {
		if (device.getId() == null || !device.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}

		Object[] values = { device.getLastIp(), device.getAppVersion(), device.getId(), device.getUser().getId() };
		
		String query = "UPDATE device SET last_access_at = now(), last_ip = ?::inet, app_version = ? "
				+ "WHERE id = ?::uuid and user_id = ?::uuid";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(e);
		}
	}

	@Override
	public void delete(UUID deviceID) throws DAOException {
		Object[] values = { deviceID };

		String query = "DELETE FROM device WHERE id = ?::uuid";

		executeUpdate(query, values);
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
