package com.stacksync.syncservice.db;

import java.util.Collection;

import com.stacksync.commons.models.Device;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface DeviceDAO {
	public Device get(Long id) throws DAOException;

	public Collection<Device> findAll() throws DAOException;

	public void add(Device device) throws DAOException;

	public void update(Device device) throws DAOException;

	public void delete(Long id) throws DAOException;

}
