package com.stacksync.syncservice.handler;

import com.stacksync.commons.exceptions.*;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.infinispan.models.*;

import java.util.List;
import java.util.UUID;

public interface SyncHandler {

	public List<CommitInfo> doCommit(UserRMI user, WorkspaceRMI workspace, DeviceRMI device, List<ItemMetadataRMI> items)
			throws Exception;
	
	public List<ItemMetadataRMI> doGetChanges(UserRMI user, WorkspaceRMI workspace);

	public UUID doUpdateDevice(DeviceRMI device) throws UserNotFoundException, DeviceNotValidException,
			DeviceNotUpdatedException;

	public List<WorkspaceRMI> doGetWorkspaces(UserRMI user) throws NoWorkspacesFoundException;

	public WorkspaceRMI doShareFolder(UserRMI user, List<String> emails, ItemRMI item, boolean isEncrypted)
			throws ShareProposalNotCreatedException, UserNotFoundException;

	public void doUpdateWorkspace(UserRMI user, WorkspaceRMI workspace) throws UserNotFoundException,
			WorkspaceNotUpdatedException;

	public UserRMI doGetUser(String email) throws UserNotFoundException;
	
	public Connection getConnection();

}
