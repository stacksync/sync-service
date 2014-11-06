package com.stacksync.syncservice.rmiserveri;

import java.rmi.Remote;
import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface UserDAORMISer extends Remote {

	public UserRMI findById(UUID id);
	
	public UserRMI getByEmail(String email);

	public List<UserRMI> findAll();
	
	public List<UserRMI> findByItemId(Long clientFileId);

	public void add(UserRMI user);

	public void update(UserRMI user);

	public void delete(UUID id);
	
}
