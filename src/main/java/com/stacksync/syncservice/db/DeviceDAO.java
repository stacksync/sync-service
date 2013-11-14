package com.stacksync.syncservice.db;

import java.util.Collection;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Device;

public interface DeviceDAO {
	public Device findByPrimaryKey(Long id) throws DAOException;

	public Device findByName(String name) throws DAOException;

	public Collection<Device> findAll() throws DAOException;

	public void add(Device device) throws DAOException;

	public void update(Device device) throws DAOException;

	public void delete(Long id) throws DAOException;

}
