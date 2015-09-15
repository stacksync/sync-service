package com.stacksync.syncservice.db;

import com.ast.cloudABE.kpabe.AttributeUpdate;
import com.ast.cloudABE.kpabe.AttributeUpdateForUser;
import com.stacksync.commons.models.ABEWorkspace;
import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
        
        public HashMap<String,LinkedList<AttributeUpdate>> getAttributeVersions(UUID workspaceId) throws DAOException;
        
        public void updateUserAttributes(UUID workspaceId, UUID userId, Collection<AttributeUpdateForUser> secretKeyComponents) throws DAOException;
    
        public HashMap<String,AttributeUpdateForUser> getUserAttributes(UUID workspaceId, UUID userId) throws DAOException;
                
        public void deleteUserAttributes(UUID workspaceId, UUID userId, ArrayList<String> attributes) throws DAOException;
                
        public void addAttributeUniverse(UUID workspaceId, Map<Integer, String> attributeUniverse) throws DAOException;

        public Map<String, Integer> getAttributeUniverse(UUID workspaceId) throws DAOException;
        
        public void updateWorkspacePublicKey(UUID workspace, byte[] publicKey) throws DAOException;
        
        public void updateWorkspaceUserSecretKey(UUID workspaceId, UUID userId, byte[] secretKey) throws DAOException;
        
        public byte[] getWorkspaceUserSecretKey(UUID workspaceId, UUID userId) throws DAOException;
                
                
}
