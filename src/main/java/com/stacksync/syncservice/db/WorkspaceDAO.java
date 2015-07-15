package com.stacksync.syncservice.db;

import com.stacksync.commons.models.ABEWorkspace;
import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface WorkspaceDAO {

	public Workspace getById(UUID id) throws DAOException;

	public List<Workspace> getByUserId(UUID userId) throws DAOException;
	
	public Workspace getDefaultWorkspaceByUserId(UUID userId) throws DAOException;
	
	public Workspace getByItemId(Long itemId) throws DAOException;

	public void add(Workspace workspace) throws DAOException;

	public void update(User user, Workspace workspace) throws DAOException;

	public void addUser(User user, Workspace workspace) throws DAOException;
        
        public void addUser(User user, ABEWorkspace workspace) throws DAOException;
	
	public void deleteUser(User user, Workspace workspace) throws DAOException;

	public void delete(UUID id) throws DAOException;
	
	public List<UserWorkspace> getMembersById(UUID workspaceId) throws DAOException;

}
