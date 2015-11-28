package com.stacksync.syncservice.handler;

import com.stacksync.commons.exceptions.*;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;

import java.util.List;
import java.util.UUID;

public interface SyncHandler {
    
   void createUser(UUID id) throws Exception;

	List<CommitInfo> doCommit(UserRMI user, WorkspaceRMI workspace, DeviceRMI device, List<ItemMetadataRMI> items)
			throws DAOException;
	
	List<ItemMetadataRMI> doGetChanges(UserRMI user, WorkspaceRMI workspace);

	UUID doUpdateDevice(DeviceRMI device) throws UserNotFoundException, DeviceNotValidException,
			DeviceNotUpdatedException;

	List<WorkspaceRMI> doGetWorkspaces(UserRMI user) throws NoWorkspacesFoundException;

	WorkspaceRMI doShareFolder(UserRMI user, List<String> emails, ItemRMI item, boolean isEncrypted)
			throws ShareProposalNotCreatedException, UserNotFoundException;

	void doUpdateWorkspace(UserRMI user, WorkspaceRMI workspace) throws UserNotFoundException,
			WorkspaceNotUpdatedException;

	UserRMI doGetUser(String email) throws UserNotFoundException;
	
	Connection getConnection();

}
