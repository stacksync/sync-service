package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.models.ItemMetadata;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.Commit;
import com.stacksync.syncservice.rpc.messages.GetWorkspaces;
import com.stacksync.syncservice.rpc.messages.GetWorkspacesResponse;

public interface Handler {

	public enum Status {
		NEW, DELETED, CHANGED, RENAMED, MOVED
	};

	public CommitResult doCommit(Commit request) throws DAOException;

	public List<ItemMetadata> doGetChanges(String workspaceName, String user);
	
	public Long doUpdateDevice(Device device);

	public APIGetMetadata ApiGetMetadata(String user, Long fileId, Boolean includeList, Boolean includeDeleted, Boolean includeChunks, Long version);

	public GetWorkspacesResponse doGetWorkspaces(GetWorkspaces workspacesRequest);

	public APICommitResponse ApiCommitMetadata(String userId, String workspaceName, Boolean overwrite, ItemMetadata fileToSave, ItemMetadata parentMetadata);

	public APICreateFolderResponse ApiCreateFolder(String strUser, String workspace, ItemMetadata objectToSave, ItemMetadata parentMetadata);

	public APIRestoreMetadata ApiRestoreMetadata(String user, String workspace, ItemMetadata object);

	public APIDeleteResponse ApiDeleteMetadata(String strUser, String workspace, ItemMetadata object);

	public APIGetVersions ApiGetVersions(String user, Long fileId);

	public Connection getConnection();

}
