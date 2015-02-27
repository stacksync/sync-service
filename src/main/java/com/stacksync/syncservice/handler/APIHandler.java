package com.stacksync.syncservice.handler;

import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import java.util.List;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;
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
	
	public APIGetMetadata getMetadata(UserRMI user, Long fileId, Boolean includeChunks, Long version, Boolean isFolder);
	
	public APICommitResponse createFile(UserRMI user, ItemMetadataRMI fileToSave);
	
	public APICommitResponse updateData(UserRMI user, ItemMetadataRMI fileToUpdate);
	
	public APICommitResponse updateMetadata(UserRMI user, ItemMetadataRMI fileToUpdate);

	public APICreateFolderResponse createFolder(UserRMI user, ItemMetadataRMI item);

	public APIRestoreMetadata restoreMetadata(UserRMI user, ItemMetadataRMI item);

	public APIDeleteResponse deleteItem(UserRMI user, ItemMetadataRMI item);

	public APIGetVersions getVersions(UserRMI user, ItemMetadataRMI item);
	
	public APIShareFolderResponse shareFolder(UserRMI user, ItemRMI item, List<String> emails);
	
	public APIUnshareFolderResponse unshareFolder(UserRMI user, ItemRMI item, List<String> emails);
	
	public APIGetWorkspaceInfoResponse getWorkspaceInfo(UserRMI user, ItemMetadataRMI item);
	
	public APIGetFolderMembersResponse getFolderMembers(UserRMI user, ItemRMI item);
}
