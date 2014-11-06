package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
//import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

//import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
//import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
//import com.stacksync.syncservice.util.Constants;

public class PostgresqlDeviceDAO extends UnicastRemoteObject implements
		DeviceDAO {

//	private static final Logger logger = Logger
//			.getLogger(PostgresqlDeviceDAO.class.getName());

	public PostgresqlDeviceDAO() throws RemoteException {
		super();
	}

	@Override
	public Device get(UUID deviceID) throws DAOException {
		Device device = null;

		return device;
	}

	@Override
	public void add(Device device) throws DAOException {

	}

	@Override
	public void update(Device device) throws DAOException {

	}

	@Override
	public void delete(UUID deviceID) throws DAOException {

	}

	private Device mapDevice(ResultSet resultSet) throws SQLException {

		Device device = new Device();

		return device;
	}

}
