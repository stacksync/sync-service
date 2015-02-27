package com.stacksync.syncservice.handler;

import java.util.List;
import java.util.UUID;

import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.infinispan.models.CommitInfoRMI;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface SyncHandler {

	public List<CommitInfoRMI> doCommit(UserRMI user, WorkspaceRMI workspace, DeviceRMI device, List<ItemMetadataRMI> items)
			throws DAOException;
	
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
