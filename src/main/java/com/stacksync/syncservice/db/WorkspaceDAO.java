package com.stacksync.syncservice.db;

import java.util.Collection;
import java.util.List;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface WorkspaceDAO {

	public Workspace findById(Long id) throws DAOException;

	public Collection<Workspace> findAll() throws DAOException;

	public List<Workspace> findByUserCloudId(String userCloudId) throws DAOException;
	
	public Workspace getByItemId(Long itemId) throws DAOException;

	public void add(Workspace workspace) throws DAOException;

	public void update(Workspace workspace) throws DAOException;

	public void addUser(User user, Workspace workspace) throws DAOException;

	public void delete(Long id) throws DAOException;

}
