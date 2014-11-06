package com.stacksync.syncservice.rmiserveri;

import java.rmi.Remote;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface UserDAORMISer extends Remote {

	public UserRMI findById(UUID id) throws DAOException;
	
	public UserRMI getByEmail(String email) throws DAOException;

	public List<UserRMI> findAll() throws DAOException;
	
	public List<UserRMI> findByItemId(Long clientFileId) throws DAOException;

	public void add(UserRMI user) throws DAOException;

	public void update(UserRMI user) throws DAOException;

	public void delete(UUID id) throws DAOException;
}
