package com.stacksync.syncservice.db;

import com.stacksync.commons.models.User;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface UserExternalDAO {


	public void add(User user) throws DAOException;
	public User getByEmail(String email) throws DAOException;

	

}
