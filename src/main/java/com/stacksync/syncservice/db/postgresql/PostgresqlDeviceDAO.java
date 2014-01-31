package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class PostgresqlDeviceDAO extends PostgresqlDAO implements DeviceDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlDeviceDAO.class.getName());

	public PostgresqlDeviceDAO(Connection connection) {
		super(connection);
	}

	@Override
	public Device get(Long deviceID) throws DAOException {
		ResultSet resultSet = null;
		Device device = null;

		String query = "SELECT * FROM device WHERE id = ?";

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
	public Collection<Device> findAll() throws DAOException {
		ResultSet resultSet = null;
		Collection<Device> list = new ArrayList<Device>();

		String query = "SELECT * FROM CLIENTS";
		try {
			resultSet = executeQuery(query, null);

			while (resultSet.next()) {
				list.add(mapDevice(resultSet));
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
		return list;
	}

	@Override
	public void add(Device device) throws DAOException {
		if (!device.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}

		Object[] values = { device.getName(), device.getUser().getId(), device.getOs(), 
				device.getLastIp(), device.getAppVersion() };

		String query = "INSERT INTO device (name, user_id, os, created_at, last_access_at, last_ip, app_version) "
				+ "VALUES (?, ?, ?, now(), now(), ?::inet, ?)";

		Long id = executeUpdate(query, values);

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
				+ "WHERE id = ? and user_id = ?";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(e);
		}
	}

	@Override
	public void delete(Long deviceID) throws DAOException {
		Object[] values = { deviceID };

		String query = "DELETE FROM device WHERE id = ?";

		executeUpdate(query, values);
	}

	private Device mapDevice(ResultSet resultSet) throws SQLException {

		Device device = new Device();
		device.setId(resultSet.getLong("id"));
		device.setName(resultSet.getString("name"));

		User user = new User();
		user.setId(resultSet.getLong("user_id"));

		device.setUser(user);

		return device;

	}

}
