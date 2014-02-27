package com.stacksync.syncservice.db;

import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface WorkspaceDAO {

	public Workspace findById(UUID id) throws DAOException;

	public List<Workspace> findByUserId(UUID userId) throws DAOException;
	
	public Workspace getByItemId(Long itemId) throws DAOException;

	public void add(Workspace workspace) throws DAOException;

	public void update(User user, Workspace workspace) throws DAOException;

	public void addUser(User user, Workspace workspace) throws DAOException;

	public void delete(UUID id) throws DAOException;

}
