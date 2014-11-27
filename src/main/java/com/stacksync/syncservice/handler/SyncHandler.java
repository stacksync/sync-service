package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.commons.models.SyncMetadata;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface SyncHandler {

	public List<CommitInfo> doCommit(User user, Workspace workspace, Device device, List<ItemMetadata> items)
			throws DAOException;
	
	public List<SyncMetadata> doGetChanges(User user, Workspace workspace);

	public UUID doUpdateDevice(Device device) throws UserNotFoundException, DeviceNotValidException,
			DeviceNotUpdatedException;

	public List<Workspace> doGetWorkspaces(User user) throws NoWorkspacesFoundException;

	public Workspace doShareFolder(User user, List<String> emails, Item item, boolean isEncrypted)
			throws ShareProposalNotCreatedException, UserNotFoundException;

	public void doUpdateWorkspace(User user, Workspace workspace) throws UserNotFoundException,
			WorkspaceNotUpdatedException;

	public User doGetUser(String email) throws UserNotFoundException;
	
	public Connection getConnection();

}
