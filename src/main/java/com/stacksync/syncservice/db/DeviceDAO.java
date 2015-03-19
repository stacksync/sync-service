package com.stacksync.syncservice.db;

import java.util.UUID;

import com.stacksync.commons.models.Device;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface DeviceDAO {
	
	public Device get(UUID userID, UUID deviceID) throws DAOException;

	public void add(Device device) throws DAOException;

	public void update(Device device) throws DAOException;

	public void delete(UUID userID, UUID deviceID) throws DAOException;

}
