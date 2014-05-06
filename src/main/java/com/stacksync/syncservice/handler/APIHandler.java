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
	
	public APIGetMetadata ApiGetMetadata(User user, Long fileId, Boolean includeList, Boolean includeDeleted,
			Boolean includeChunks, Long version);
	
	public APICommitResponse ApiCommitMetadata(User user, Boolean overwrite, ItemMetadata fileToSave,
			ItemMetadata parentMetadata);

	public APICreateFolderResponse ApiCreateFolder(User user, ItemMetadata item);

	public APIRestoreMetadata ApiRestoreMetadata(User user, ItemMetadata item);

	public APIDeleteResponse ApiDeleteMetadata(User user, ItemMetadata item);

	public APIGetVersions ApiGetVersions(User user, ItemMetadata item);
	
	public APIUserMetadata ApiGetUserMetadata(User user);
}
