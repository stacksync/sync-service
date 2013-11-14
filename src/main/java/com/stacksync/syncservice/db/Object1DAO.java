package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.models.ObjectMetadata;

public interface Object1DAO {
	public Object1 findByPrimaryKey(Long id) throws DAOException;

	public Object1 findByClientId(long clientID) throws DAOException;

	public Object1 findByClientFileIdAndWorkspace(Long clientFileID, Long workspaceId) throws DAOException;

	public List<Object1> findAll() throws DAOException;

	public List<Object1> findByWorkspaceId(long workspaceID) throws DAOException;

	public List<Object1> findByWorkspaceName(String workspaceName) throws DAOException;

	public void add(Object1 object1) throws DAOException;

	public void update(Object1 object1) throws DAOException;

	public void put(Object1 object) throws DAOException;

	public void delete(Long id) throws DAOException;

	// ObjectMetadata information
	public List<ObjectMetadata> getObjectMetadataByWorkspaceName(String workspaceName) throws DAOException;

	public List<ObjectMetadata> getObjectsByClientFileId(Long fileId) throws DAOException;

	public ObjectMetadata findByClientFileId(Long fileId, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks) throws DAOException;

	public ObjectMetadata findByServerUserId(String serverUserId, Boolean includeDeleted) throws DAOException;

	public ObjectMetadata findObjectVersionsByClientFileId(Long fileId) throws DAOException;

}
