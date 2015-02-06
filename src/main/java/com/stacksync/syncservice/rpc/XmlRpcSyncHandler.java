package com.stacksync.syncservice.rpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import omq.common.broker.Broker;
import omq.exception.RemoteException;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.SharingProposal;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.notifications.CommitNotification;
import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.notifications.UnshareNotification;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.commons.omq.RemoteWorkspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.handler.SQLAPIHandler;
import com.stacksync.syncservice.handler.Handler.Status;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetFolderMembersResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIGetWorkspaceInfoResponse;
import com.stacksync.syncservice.rpc.messages.APIResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIShareFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIUnshareFolderResponse;
import com.stacksync.syncservice.rpc.parser.IParser;
import com.stacksync.syncservice.util.Constants;

public class XmlRpcSyncHandler {

	private static final Logger logger = Logger.getLogger(XmlRpcSyncHandler.class.getName());
	private SQLAPIHandler apiHandler;
	private IParser parser;
	private Broker broker;

	public XmlRpcSyncHandler(Broker broker, ConnectionPool pool) {
		try {
			this.apiHandler = new SQLAPIHandler(pool);
			this.broker = broker;
			this.parser = Reader.getInstance("com.stacksync.syncservice.rpc.parser.JSONParser");

			logger.info("XMLRPC server set up done.");
		} catch (Exception e) {
			logger.error("XMLRPC server could not initiliaze.");
		}
	}

	public String getMetadata(String strUserId, String strItemId, String strIncludeChunks, String strVersion,
			String strIsFolder) {

		logger.debug(String.format("XMLRPC Request. getMetadata [userId: %s, fileId: %s, chunks: %s, version: %s]",
				strUserId, strItemId, strIncludeChunks, strVersion));

		Long fileId = null;
		try {
			fileId = Long.parseLong(strItemId);
		} catch (NumberFormatException ex) {
		}

		Boolean isFolder = Boolean.parseBoolean(strIsFolder);

		Boolean includeChunks = Boolean.parseBoolean(strIncludeChunks);

		Long version = null;
		try {
			version = Long.parseLong(strVersion);
		} catch (NumberFormatException ex) {
		}

		User user = new User();
		user.setId(UUID.fromString(strUserId));

		APIGetMetadata response = this.apiHandler.getMetadata(user, fileId, includeChunks, version, isFolder);

		logger.debug(String.format("XMLRPC Response. %s", response.toString()));

		return response.toString();
	}

	public String getFolderContents(String strUserId, String strFolderId, String strIncludeDeleted) {

		Boolean includeList = true;

		logger.debug(String.format("XMLRPC Request. getMetadata [userId: %s, fileId: %s, includeList: %s]", strUserId,
				strFolderId, includeList, strIncludeDeleted));

		Long folderId = null;
		try {
			folderId = Long.parseLong(strFolderId);
		} catch (NumberFormatException ex) {
		}

		Boolean includeDeleted = Boolean.parseBoolean(strIncludeDeleted);

		User user = new User();
		user.setId(UUID.fromString(strUserId));

		APIGetMetadata response = this.apiHandler.getFolderContent(user, folderId, includeDeleted);

		logger.debug(String.format("XMLRPC Response. %s", response.toString()));

		return response.toString();
	}

	public String getVersions(String strUserId, String strFileId) {

		Long itemId = null;
		try {
			itemId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		// TODO: filtrar versiones borradas!!
		logger.debug("XMLRPC -> get_versions -->[User:" + strUserId + ", itemId:" + itemId + "]");

		User user = new User();
		user.setId(UUID.fromString(strUserId));

		ItemMetadata item = new ItemMetadata();
		item.setId(itemId);

		APIGetVersions response = this.apiHandler.getVersions(user, item);

		logger.debug("XMLRPC -> resp -->[" + response.toString() + "]");
		return response.toString();
	}

	public String newFolder(String strUserId, String strFolderName, String strParentId) {

		logger.debug(String.format("XMLRPC Request. createFolder [userId: %s, folderName: %s, parentId: %s]",
				strUserId, strFolderName, strParentId));

		Long parentId = null;
		try {
			parentId = Long.parseLong(strParentId);
		} catch (NumberFormatException ex) {
		}

		if (strFolderName.length() == 0) {
			APIResponse response = new APICreateFolderResponse(null, false, 400, "Folder name cannot be empty.");
			String strResponse = this.parser.createResponse(response);
			return strResponse;
		}

		User user = new User();
		user.setId(UUID.fromString(strUserId));

		ItemMetadata item = new ItemMetadata();
		item.setFilename(strFolderName);
		item.setIsFolder(true);
		item.setParentId(parentId);

		APIResponse response = this.apiHandler.createFolder(user, item);

		String workspace = response.getItem().getMetadata().getWorkspaceId().toString();

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, response);
		}

		logger.debug("XMLRPC -> resp -->[" + response.toString() + "]");

		return response.toString();
	}

	public String newFile(String strUserId, String strFileName, String strParentId, String strChecksum,
			String strFileSize, String strMimetype, List<String> chunks) {

		Long parentId = null;
		try {
			parentId = Long.parseLong(strParentId);
		} catch (NumberFormatException ex) {
		}

		Long checksum = null;
		try {
			checksum = Long.parseLong(strChecksum);
		} catch (NumberFormatException ex) {
		}

		Long fileSize = null;
		try {
			fileSize = Long.parseLong(strFileSize);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> put_metadata_file -->[User:" + strUserId + ", FileName:" + strFileName + ", parentId: "
				+ strParentId + "]");

		UUID userId = UUID.fromString(strUserId);

		APIGetMetadata metadataResponse = getParentMetadata(userId, parentId);
		APICommitResponse parentResponse = checkParentMetadata(parentId, metadataResponse);

		if (!parentResponse.getSuccess()) {// error
			logger.debug("XMLRPC -> Error resp -->[" + parentResponse.toString() + "]");
			return parentResponse.toString();
		}

		ItemMetadata item = new ItemMetadata();

		item.setId(null);
		item.setParentId(parentId);
		item.setTempId(new Random().nextLong());
		item.setVersion(1L);
		item.setDeviceId(Constants.API_DEVICE_ID);
		item.setIsFolder(false);
		item.setStatus(Status.NEW.toString());
		item.setFilename(strFileName);
		item.setSize(fileSize);
		item.setChecksum(checksum);
		item.setMimetype(strMimetype);
		item.setModifiedAt(new Date());
		item.setChunks(chunks);

		User user = new User();
		user.setId(userId);

		APICommitResponse response = this.apiHandler.createFile(user, item);

		if (response.getSuccess()) {
			this.sendMessageToClients(response.getMetadata().getWorkspaceId().toString(), response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String updateData(String strUserId, String strFileId, String strChecksum, String strFileSize,
			String strMimetype, List<String> chunks) {

		logger.debug("XMLRPC -> update data -->[User:" + strUserId + ", Checksum: " + strChecksum + ", Filesize: "
				+ strFileSize + ", Mimetype: " + strMimetype + ", Chunks: " + chunks + "]");

		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		Long checksum = null;
		try {
			checksum = Long.parseLong(strChecksum);
		} catch (NumberFormatException ex) {
		}

		Long fileSize = null;
		try {
			fileSize = Long.parseLong(strFileSize);
		} catch (NumberFormatException ex) {
		}

		UUID userId = UUID.fromString(strUserId);

		ItemMetadata item = new ItemMetadata();

		item.setId(fileId);
		item.setSize(fileSize);
		item.setChecksum(checksum);
		item.setMimetype(strMimetype);
		item.setChunks(chunks);
		item.setStatus(Status.CHANGED.toString());
		User user = new User();
		user.setId(userId);

		APICommitResponse response = this.apiHandler.updateData(user, item);

		if (response.getSuccess()) {
			this.sendMessageToClients(response.getMetadata().getWorkspaceId().toString(), response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;

	}

	public String updateMetadata(String strUserId, String strFileId, String strNewFileName, String strNewParentId) {

		logger.debug("XMLRPC -> put_metadata_file -->[User:" + strUserId + ", FileName:" + strNewFileName
				+ ", parentId: " + strNewParentId + "]");

		Long parentId = null;
		try {
			parentId = Long.parseLong(strNewParentId);
		} catch (NumberFormatException ex) {
		}

		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		UUID userId = UUID.fromString(strUserId);

		User user = new User();
		user.setId(userId);

		ItemMetadata file = new ItemMetadata();
		file.setId(fileId);
		file.setFilename(strNewFileName);
		file.setParentId(parentId);

		APICommitResponse response = this.apiHandler.updateMetadata(user, file);

		if (response.getSuccess()) {
			this.sendMessageToClients(response.getMetadata().getWorkspaceId().toString(), response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String deleteItem(String strUserId, String strFileId, String strIsFolder) {
		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		Boolean isFolder = Boolean.parseBoolean(strIsFolder);

		logger.debug("XMLRPC -> delete_metadata_file -->[User:" + strUserId + ", fileId:" + fileId + "]");

		ItemMetadata object = new ItemMetadata();
		object.setId(fileId);
		object.setIsFolder(isFolder);

		User user = new User();
		user.setId(UUID.fromString(strUserId));

		APIDeleteResponse response = this.apiHandler.deleteItem(user, object);

		if (response.getSuccess()) {
			this.sendMessageToClients(response.getMetadata().getWorkspaceId().toString(), response);
		}

		logger.debug("XMLRPC -> resp -->[" + response.toString() + "]");
		return response.toString();
	}

	// necessary?
	public String restoreMetadata(UUID userId, String strRequestId, String strFileId, String strVersion) {
		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		Long version = null;
		try {
			version = Long.parseLong(strVersion);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> restore_file -->[User:" + userId + ", Request:" + strRequestId + ", fileId:"
				+ strFileId + ", version: " + strVersion + "]");

		String workspace = userId + "/";

		ItemMetadata object = new ItemMetadata();
		object.setId(fileId);
		object.setVersion(version);

		User user = new User();
		user.setId(userId);

		APIRestoreMetadata response = this.apiHandler.restoreMetadata(user, object);
		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, response);
		}

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String shareFolder(String strUserId, String strFolderId, List<String> emails) {

		logger.debug("XMLRPC -> share_folder -->[User:" + strUserId + ", Folder ID:" + strFolderId + ", Emails: "
				+ emails.toString() + "]");

		Long folderId = null;
		try {
			folderId = Long.parseLong(strFolderId);
		} catch (NumberFormatException ex) {
		}

		UUID userId = UUID.fromString(strUserId);

		User user = new User();
		user.setId(userId);

		Item item = new Item();
		item.setId(folderId);

		APIShareFolderResponse response = this.apiHandler.shareFolder(user, item, emails);

		if (response.getSuccess()) {
			// FIXME: Do the user-workspace bindings before
			this.bindUsersToWorkspace(response.getWorkspace(), folderId);
		}

		String strResponse = response.toString();

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;

	}
	
	public String unshareFolder(String strUserId, String strFolderId, List<String> emails){
		
		logger.debug("XMLRPC -> unshare_folder --> [User:" + strUserId + ", Folder ID:" + strFolderId
				+ ", Emails: " + emails.toString() + "]");

		Long folderId = null;
		try {
			folderId = Long.parseLong(strFolderId);
		} catch (NumberFormatException ex) { }

		UUID userId = UUID.fromString(strUserId);

		User user = new User();
		user.setId(userId);

		Item item = new Item();
		item.setId(folderId);

		APIUnshareFolderResponse response = this.apiHandler.unshareFolder(user, item, emails);
			
		if (response.getSuccess()) {
			//FIXME: Do the user-workspace unbindings before
			this.unBindUsersToWorkspace(response.getWorkspace(),response.getUsersToRemove(), response.isUnshared(), folderId);
//			this.sendMessageToClients(response.getWorkspace().getId().toString(), response);
		}

		String strResponse = response.toString();

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
		
	}

	public String getFolderMembers(String strUserId, String strFolderId) {

		logger.debug("XMLRPC -> get_folder_members -->[User:" + strUserId + ", Folder ID:" + strFolderId + "]");

		Long folderId = null;
		try {
			folderId = Long.parseLong(strFolderId);
		} catch (NumberFormatException ex) {
		}

		UUID userId = UUID.fromString(strUserId);

		User user = new User();
		user.setId(userId);

		Item item = new Item();
		item.setId(folderId);

		APIGetFolderMembersResponse response = this.apiHandler.getFolderMembers(user, item);

		String strResponse = response.toString();

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;

	}

	public String getWorkspaceInfo(String strUserId, String strFileId) {

		logger.debug("XMLRPC -> get workspace info -->[User:" + strUserId + ", File ID: " + strFileId + "]");

		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		UUID userId = UUID.fromString(strUserId);

		ItemMetadata item = new ItemMetadata();

		item.setId(fileId);

		User user = new User();
		user.setId(userId);

		APIGetWorkspaceInfoResponse response = this.apiHandler.getWorkspaceInfo(user, item);

		logger.debug("XMLRPC -> resp --> [" + response.toString() + "]");
		return response.toString();

	}

	public String addExternalUserToWorkspace(String strKeyProposal){
		logger.debug("XMLRPC -> add external user to workspace -->[Share Key: " + strKeyProposal + "]");
		
		UUID key = UUID.fromString(strKeyProposal);
		SharingProposal proposal = new SharingProposal();
		proposal.setKey(key);
		
		APIShareFolderResponse response = this.apiHandler.addExternalUserToWorkspace(proposal);
		
		//if (response.getSuccess()) {
			// FIXME: Do the user-workspace bindings before
			//this.bindUsersToWorkspace(response.getWorkspace(), folderId);
		//}

		String strResponse = response.toString();

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
		
	}
	private APIGetMetadata getParentMetadata(UUID userId, Long parentId) {
		Boolean includeDeleted = true;

		User user = new User();
		user.setId(userId);

		APIGetMetadata metadataResponse = this.apiHandler.getFolderContent(user, parentId, includeDeleted);

		return metadataResponse;
	}
	
	
	private APICommitResponse checkParentMetadata(Long parentId, APIGetMetadata metadataResponse) {
		APICommitResponse response = new APICommitResponse(null, true, 0, null);
		ItemMetadata parentMetadata = metadataResponse.getItemMetadata();

		if (parentId != null && parentMetadata == null) {
			response = new APICommitResponse(null, false, 404, "Parent not found.");
		} else if (!parentMetadata.isFolder()) {
			response = new APICommitResponse(null, false, 400, "Incorrect parent.");
		} else if (!parentMetadata.isRoot() && parentMetadata.getStatus().equals("DELETED")) {
			response = new APICommitResponse(null, false, 400, "Parent is deleted.");
		} else if (!metadataResponse.getSuccess()) {
			response = new APICommitResponse(null, metadataResponse.getSuccess(), metadataResponse.getErrorCode(),
					metadataResponse.getDescription());
		}

		return response;
	}
	
	private void bindUsersToWorkspace(Workspace workspace, Long folderId) {
		
		// Create notification
		ShareProposalNotification notification = new ShareProposalNotification(workspace.getId(),
				workspace.getName(), folderId, workspace.getOwner().getId(), workspace.getOwner().getName(),
				workspace.getSwiftContainer(), workspace.getSwiftUrl(), workspace.isEncrypted());

		notification.setRequestId("");

		// Send notification to owner
		RemoteClient client;
		try {
			client = broker.lookupMulti(workspace.getOwner().getId().toString(), RemoteClient.class);
			client.notifyShareProposal(notification);
		} catch (RemoteException e1) {
			logger.error(String.format("Could not notify user: '%s'", workspace.getOwner().getId()), e1);
		}

		// Send notifications to users
		for (User addressee : workspace.getUsers()) {
			try {
				client = broker.lookupMulti(addressee.getId().toString(), RemoteClient.class);
				client.notifyShareProposal(notification);
			} catch (RemoteException e) {
				logger.error(String.format("Could not notify user: '%s'", addressee.getId()), e);
			}
		}

	}
	private void unBindUsersToWorkspace(Workspace workspace, List<User> usersToRemove, boolean isUnshared, Long folderId) {
		
		// Create notification
		UnshareNotification notification = new UnshareNotification(workspace.getId(),
				workspace.getName(), folderId, workspace.getOwner().getId(), workspace.getOwner().getName(),
				workspace.getSwiftContainer(), workspace.getSwiftUrl(), workspace.isEncrypted());

		notification.setRequestId("");
		RemoteClient client;
		// Send notification to owner
		if (isUnshared){
			try {
				client = broker.lookupMulti(workspace.getOwner().getId().toString(), RemoteClient.class);
				client.notifyUnshare(notification);
			} catch (RemoteException e1) {
				logger.error(String.format("Could not notify user: '%s'", workspace.getOwner().getId()), e1);
			}
		}
	

		// Send notifications to users
		for (User addressee : usersToRemove) {
			try {
				client = broker.lookupMulti(addressee.getId().toString(), RemoteClient.class);
				client.notifyUnshare(notification);
			} catch (RemoteException e) {
				logger.error(String.format("Could not notify user: '%s'", addressee.getId()), e);
			}
		} 	
	}
	private void sendMessageToClients(String workspaceName, APIResponse generalResponse) {

		CommitInfo info = generalResponse.getItem();
		List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();
		responseObjects.add(info);
		CommitNotification result = new CommitNotification("", responseObjects);

		RemoteWorkspace commitNotifier;
		try {
			commitNotifier = broker.lookupMulti(workspaceName, RemoteWorkspace.class);
			commitNotifier.notifyCommit(result);
		} catch (RemoteException e) {
			// e.printStackTrace();
			logger.error("Error sending the notification to the clients: " + e);
		}

	}
}
