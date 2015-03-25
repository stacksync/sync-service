package com.stacksync.syncservice.handler;

import java.util.List;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetFolderMembersResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIGetWorkspaceInfoResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIShareFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIUnshareFolderResponse;

public interface APIHandler {
	
	public APIGetMetadata getMetadata(User user, Long fileId, Boolean includeChunks, Long version, Boolean isFolder);
	
	public APICommitResponse createFile(User user, ItemMetadata fileToSave);
	
	public APICommitResponse updateData(User user, ItemMetadata fileToUpdate);
	
	public APICommitResponse updateMetadata(User user, ItemMetadata fileToUpdate, Boolean parentUpdated);

	public APICreateFolderResponse createFolder(User user, ItemMetadata item);

	public APIRestoreMetadata restoreMetadata(User user, ItemMetadata item);

	public APIDeleteResponse deleteItem(User user, ItemMetadata item);

	public APIGetVersions getVersions(User user, ItemMetadata item);
	
	public APIShareFolderResponse shareFolder(User user, Item item, List<String> emails);
	
	public APIUnshareFolderResponse unshareFolder(User user, Item item, List<String> emails);
	
	public APIGetWorkspaceInfoResponse getWorkspaceInfo(User user, ItemMetadata item);
	
	public APIGetFolderMembersResponse getFolderMembers(User user, Item item);
}
