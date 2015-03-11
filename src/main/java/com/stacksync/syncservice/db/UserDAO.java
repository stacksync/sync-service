package com.stacksync.syncservice.db;

import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface UserDAO {

	public User findById(UUID id) throws DAOException;
	
	public User getByEmail(String email) throws DAOException;

	public List<User> findAll() throws DAOException;
	
	public List<User> findByItemId(Long clientFileId) throws DAOException;

	public void add(User user) throws DAOException;

	public void update(User user) throws DAOException;

	public void delete(UUID id) throws DAOException;
		
	public void updateAvailableQuota(User user) throws DAOException;
}
