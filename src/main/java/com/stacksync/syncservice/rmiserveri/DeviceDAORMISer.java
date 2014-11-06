package com.stacksync.syncservice.rmiserveri;

import java.util.UUID;

import com.stacksync.commons.models.Device;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface DeviceDAORMISer {
	
	public Device get(UUID id) throws DAOException;

	public void add(Device device) throws DAOException;

	public void update(Device device) throws DAOException;

	public void delete(UUID id) throws DAOException;

}
