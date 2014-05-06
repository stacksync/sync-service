package com.stacksync.syncservice.handler;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIUserMetadata;

public interface APIHandler {
	
	public APIGetMetadata getMetadata(User user, Long fileId, Boolean includeChunks, Long version);
	
	public APICommitResponse ApiCommitMetadata(User user, Boolean overwrite, ItemMetadata fileToSave,
			ItemMetadata parentMetadata);

	public APICreateFolderResponse createFolder(User user, ItemMetadata item);

	public APIRestoreMetadata ApiRestoreMetadata(User user, ItemMetadata item);

	public APIDeleteResponse deleteItem(User user, ItemMetadata item);

	public APIGetVersions getVersions(User user, ItemMetadata item);
	
	public APIUserMetadata ApiGetUserMetadata(User user);
}
