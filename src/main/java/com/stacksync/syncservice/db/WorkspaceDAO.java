package com.stacksync.syncservice.db;

import java.util.Collection;
import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;

public interface WorkspaceDAO {

	public Workspace findByPrimaryKey(Long id) throws DAOException;

	public Workspace findByName(String workspaceName) throws DAOException;

	public Long getPrimaryKey(String workspaceName) throws DAOException;

	public Collection<Workspace> findAll() throws DAOException;

	public List<Workspace> findByUserCloudId(String userCloudId) throws DAOException;

	public void add(Workspace workspace) throws DAOException;

	public void update(Workspace workspace) throws DAOException;

	public void addUser(User user, Workspace workspace, String path) throws DAOException;

	public void delete(Long id) throws DAOException;

}
