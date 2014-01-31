package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.util.List;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.syncservice.exceptions.NoWorkspacesFoundException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;

public interface Handler {

	public enum Status {
		NEW, DELETED, CHANGED, RENAMED, MOVED
	};

	public List<CommitInfo> doCommit(User user, Workspace workspace, Device device, List<ItemMetadata> items) throws DAOException;

	public List<ItemMetadata> doGetChanges(User user, Workspace workspace);
	
	public Long doUpdateDevice(Device device) throws UserNotFoundException, DeviceNotValidException, DeviceNotUpdatedException;

	public APIGetMetadata ApiGetMetadata(User user, Long fileId, Boolean includeList, Boolean includeDeleted, Boolean includeChunks, Long version);

	public List<Workspace> doGetWorkspaces(User user) throws NoWorkspacesFoundException;

	public APICommitResponse ApiCommitMetadata(User user, Boolean overwrite, ItemMetadata fileToSave, ItemMetadata parentMetadata);

	public APICreateFolderResponse ApiCreateFolder(User user, ItemMetadata itemToSave, ItemMetadata parentMetadata);

	public APIRestoreMetadata ApiRestoreMetadata(User user, ItemMetadata item);

	public APIDeleteResponse ApiDeleteMetadata(User user, ItemMetadata item);

	public APIGetVersions ApiGetVersions(User user, ItemMetadata item);

	public Connection getConnection();

	public Long doCreateShareProposal(User user, List<String> emails, String folderName);

}
