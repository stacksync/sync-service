package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.User;

public interface UserDAO {

	public User findByPrimaryKey(Long id) throws DAOException;

	public User findByCloudId(String cloudId) throws DAOException;

	public List<User> findAll() throws DAOException;
	
	public List<User> findByClientFileId(Long clientFileId) throws DAOException;

	public void add(User user) throws DAOException;

	public void update(User user) throws DAOException;

	public void delete(Long id) throws DAOException;

	public void delete(String cloudId) throws DAOException;

}
