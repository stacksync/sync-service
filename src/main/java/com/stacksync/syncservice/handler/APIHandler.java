package com.stacksync.syncservice.handler;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIGetWorkspaceInfoResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIUserMetadata;

public interface APIHandler {
	
	public APIGetMetadata getMetadata(User user, Long fileId, Boolean includeChunks, Long version, Boolean isFolder);
	
	public APICommitResponse createFile(User user, ItemMetadata fileToSave);
	
	public APICommitResponse updateData(User user, ItemMetadata fileToUpdate);
	
	public APICommitResponse updateMetadata(User user, ItemMetadata fileToUpdate);

	public APICreateFolderResponse createFolder(User user, ItemMetadata item);

	public APIRestoreMetadata restoreMetadata(User user, ItemMetadata item);

	public APIDeleteResponse deleteItem(User user, ItemMetadata item);

	public APIGetVersions getVersions(User user, ItemMetadata item);
	
	public APIGetWorkspaceInfoResponse getWorkspaceInfo(User user, ItemMetadata item);
}
