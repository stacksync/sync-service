package com.stacksync.syncservice.handler;

import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.rpc.messages.*;

import java.util.List;

public interface APIHandler {
	
	APIGetMetadata getMetadata(UserRMI user, Long fileId, Boolean includeChunks, Long version, Boolean isFolder);
	APICommitResponse createFile(UserRMI user, ItemMetadataRMI fileToSave);
	APICommitResponse updateData(UserRMI user, ItemMetadataRMI fileToUpdate);
	APICommitResponse updateMetadata(UserRMI user, ItemMetadataRMI fileToUpdate);
	APICreateFolderResponse createFolder(UserRMI user, ItemMetadataRMI item);
	APIRestoreMetadata restoreMetadata(UserRMI user, ItemMetadataRMI item);
	APIDeleteResponse deleteItem(UserRMI user, ItemMetadataRMI item);
	APIGetVersions getVersions(UserRMI user, ItemMetadataRMI item);
	APIShareFolderResponse shareFolder(UserRMI user, ItemRMI item, List<String> emails);
	APIUnshareFolderResponse unshareFolder(UserRMI user, ItemRMI item, List<String> emails);
	APIGetWorkspaceInfoResponse getWorkspaceInfo(UserRMI user, ItemMetadataRMI item);
	APIGetFolderMembersResponse getFolderMembers(UserRMI user, ItemRMI item);
}
