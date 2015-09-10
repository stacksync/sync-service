package com.stacksync.syncservice.db;

import com.ast.cloudABE.kpabe.AttributeUpdate;
import com.stacksync.commons.models.ABEWorkspace;
import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import java.util.ArrayList;
import java.util.Map;

public interface WorkspaceDAO {

	public Workspace getById(UUID id) throws DAOException;

	public List<Workspace> getByUserId(UUID userId) throws DAOException;
	
	public Workspace getDefaultWorkspaceByUserId(UUID userId) throws DAOException;
	
	public Workspace getByItemId(Long itemId) throws DAOException;

	public void add(Workspace workspace) throws DAOException;
        
        public void add(ABEWorkspace workspace) throws DAOException;

	public void update(User user, Workspace workspace) throws DAOException;

	public void addUser(User user, Workspace workspace) throws DAOException;
        
        public void addUser(User user, ABEWorkspace workspace) throws DAOException;
	
	public void deleteUser(User user, Workspace workspace) throws DAOException;

	public void delete(UUID id) throws DAOException;
	
	public List<UserWorkspace> getMembersById(UUID workspaceId) throws DAOException;
        
        public void addAttributeVersions(UUID workspaceId, ArrayList<AttributeUpdate> attributeVersions) throws DAOException;
        
        public void updateUserAttributes(UUID workspaceId, UUID userId, Map<String, Map<Long, byte[]>> secretKeyComponents) throws DAOException;
    
        public void addAttributeUniverse(UUID workspaceId, Map<String, Integer> attributeUniverse) throws DAOException;

}
