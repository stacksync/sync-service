package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface UserDAO {

	public User findByPrimaryKey(Long id) throws DAOException;

	public User findByCloudId(String cloudId) throws DAOException;
	
	public User findByEmail(String email) throws DAOException;

	public List<User> findAll() throws DAOException;
	
	public List<User> findByItemId(Long clientFileId) throws DAOException;

	public void add(User user) throws DAOException;

	public void update(User user) throws DAOException;

	public void delete(Long id) throws DAOException;

	public void delete(String cloudId) throws DAOException;
}
