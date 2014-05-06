package com.stacksync.syncservice.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import omq.common.broker.Broker;
import omq.exception.RemoteException;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.notifications.CommitNotification;
import com.stacksync.commons.omq.RemoteWorkspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLAPIHandler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.parser.IParser;

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

	public String getMetadata(String strUserId, String strItemId, String strIncludeChunks, String strVersion) {

		logger.debug(String
				.format("XMLRPC Request. getMetadata [userId: %s, fileId: %s, chunks: %s, version: %s]",
						strUserId, strItemId, strIncludeChunks, strVersion));

		Long fileId = null;
		try {
			fileId = Long.parseLong(strItemId);
		} catch (NumberFormatException ex) {
		}

		Boolean includeChunks = Boolean.parseBoolean(strIncludeChunks);

		Long version = null;
		try {
			version = Long.parseLong(strVersion);
		} catch (NumberFormatException ex) {
		}

		User user = new User();
		user.setId(UUID.fromString(strUserId));

		APIGetMetadata response = this.apiHandler.getMetadata(user, fileId, includeChunks, version);

		logger.debug(String.format("XMLRPC Response. %s", response.toString()));

		return response.toString();
	}
	
	public String getFolderContents(String strUserId, String strFolderId, String strIncludeDeleted) {

		Boolean includeList = true;
		
		logger.debug(String
				.format("XMLRPC Request. getMetadata [userId: %s, fileId: %s, includeList: %s]",
						strUserId, strFolderId, includeList, strIncludeDeleted));

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
	
	public String getVersions(UUID userId, String strFileId) {

		Long itemId = null;
		try {
			itemId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		// TODO: filtrar versiones borradas!!
		logger.debug("XMLRPC -> get_versions -->[User:" + userId + ", itemId:" + itemId
				+ "]");

		User user = new User();
		user.setId(userId);

		ItemMetadata item = new ItemMetadata();
		item.setId(itemId);

		APIGetVersions response = this.apiHandler.ApiGetVersions(user, item);
		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
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

		APIResponse response = this.apiHandler.ApiCreateFolder(user, item);

		String workspace = response.getItem().getMetadata().getWorkspaceId().toString();

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, response);
		}

		String strResponse = this.parser.createResponse(response);
		
		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");

		return strResponse;
	}

	// This is a TOUCH in the server!!!
	public String newFile(String strUserId, String strFileName, String strParentId) {

		Long parentId = null;
		try {
			parentId = Long.parseLong(strParentId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> put_metadata_file -->[User:" + strUserId + ", FileName:"
				+ strFileName + ", parentId: " + strParentId + "]");

		UUID userId = UUID.fromString(strUserId);
		
		APIGetMetadata metadataResponse = getParentMetadata(userId, parentId);
		String strParentResponse = checkParentMetadata(parentId, metadataResponse);

		if (strParentResponse.length() > 0) {// error
			logger.debug("XMLRPC -> Error resp -->[" + strParentResponse + "]");
			return strParentResponse;
		}

		/*
		ItemMetadata parentItem = metadataResponse.getItemMetadata();
		ItemMetadata item = new ItemMetadata();

		item.setFilename(strFileName);
		item.setSize(fileSize);
		item.setChecksum(checksum);
		item.setMimetype(strMimetype);
		item.setChunks(strChunks);

		User user = new User();
		user.setId(userId);

		APICommitResponse response = this.handler.ApiCommitMetadata(user, overwrite, item, parentItem);
		*/

		// Call a handler function: this.handler.touchFile(....)
		
		if (response.getSuccess()) {
			this.sendMessageToClients(null, response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}
	
	public String updateData(String strUserId, String strFileId, String strParentId, String strChecksum, 
			String strFileSize, String strMimetype, List<String> strChunks) {
		
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

		Long parentId = null;
		try {
			parentId = Long.parseLong(strParentId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> put_metadata_file -->[User:" + strUserId + ", parentId: " + strParentId 
				+ ", Checksum: " + checksum + ", Filesize: " + fileSize + ", Mimetype: " + strMimetype
				+ ", Chunks: " + strChunks + "]");

		UUID userId = UUID.fromString(strUserId);
		
		APIGetMetadata metadataResponse = getParentMetadata(userId, parentId);
		String strParentResponse = checkParentMetadata(parentId, metadataResponse);

		if (strParentResponse.length() > 0) {// error
			logger.debug("XMLRPC -> Error resp -->[" + strParentResponse + "]");
			return strParentResponse;
		}

		/*
		ItemMetadata parentItem = metadataResponse.getItemMetadata();
		ItemMetadata item = new ItemMetadata();

		item.setFilename(strFileName);
		item.setSize(fileSize);
		item.setChecksum(checksum);
		item.setMimetype(strMimetype);
		item.setChunks(strChunks);

		User user = new User();
		user.setId(userId);

		APICommitResponse response = this.handler.ApiCommitMetadata(user, overwrite, item, parentItem);
		*/
		
		// Call handler function with new metadata
		
		if (response.getSuccess()) {
			this.sendMessageToClients(null, response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}
	
	public String updateMetadata(String strUserId, String strFileId, String strNewFileName, String strNewParentId) {

		Long parentId = null;
		try {
			parentId = Long.parseLong(strNewParentId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> put_metadata_file -->[User:" + strUserId + ", FileName:"
				+ strNewFileName + ", parentId: " + strNewParentId + "]");

		UUID userId = UUID.fromString(strUserId);
		
		APIGetMetadata metadataResponse = getParentMetadata(userId, parentId);
		String strParentResponse = checkParentMetadata(parentId, metadataResponse);

		if (strParentResponse.length() > 0) {// error
			logger.debug("XMLRPC -> Error resp -->[" + strParentResponse + "]");
			return strParentResponse;
		}
		
		/*
		ItemMetadata parentItem = metadataResponse.getItemMetadata();
		ItemMetadata item = new ItemMetadata();

		item.setFilename(strNewFileName);
		item.setSize(fileSize);
		item.setChecksum(checksum);
		item.setMimetype(strMimetype);
		item.setChunks(strChunks);

		User user = new User();
		user.setId(userId);

		APICommitResponse response = this.handler.ApiCommitMetadata(user, overwrite, item, parentItem);
		*/
		
		// 1. Check if want to update filename, parent or both.
		// 2. Call handler function: this.handler.ApiUpdateMetadata(....);
		
		if (response.getSuccess()) {
			this.sendMessageToClients(null, response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String deleteItem(UUID userId, String strFileId) {
		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> delete_metadata_file -->[User:" + userId + ", fileId:"	+ fileId + "]");

		ItemMetadata object = new ItemMetadata();
		object.setId(fileId);

		User user = new User();
		user.setId(userId);

		APIDeleteResponse response = this.apiHandler.ApiDeleteMetadata(user, object);
		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(null, response);
		}

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
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

		APIRestoreMetadata response = this.apiHandler.ApiRestoreMetadata(user, object);
		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, response);
		}

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}
	
	private APIGetMetadata getParentMetadata(UUID userId, Long parentId) {
		Long version = null;
		Boolean list = true;
		Boolean includeDeleted = true;
		Boolean includeChunks = false;

		User user = new User();
		user.setId(userId);

		APIGetMetadata metadataResponse = this.apiHandler.ApiGetMetadata(user, parentId, list, includeDeleted,
				includeChunks, version);

		return metadataResponse;
	}

	private String checkParentMetadata(Long parentId, APIGetMetadata metadataResponse) {
		String strResponse = "";
		ItemMetadata parentMetadata = metadataResponse.getItemMetadata();

		if (parentId != null && parentMetadata == null) {
			APICommitResponse response = new APICommitResponse(null, false, 404, "Parent not found.");

			strResponse = this.parser.createResponse(response);
		} else if (!parentMetadata.isFolder()) {
			APICommitResponse response = new APICommitResponse(null, false, 400, "Incorrect parent.");

			strResponse = this.parser.createResponse(response);
		} else if (!parentMetadata.isRoot() && parentMetadata.getStatus().equals("DELETED")) {
			APICommitResponse response = new APICommitResponse(null, false, 400, "Parent is deleted.");

			strResponse = this.parser.createResponse(response);
		} else if (!metadataResponse.getSuccess()) {
			APICommitResponse response = new APICommitResponse(null, metadataResponse.getSuccess(),
					metadataResponse.getErrorCode(), metadataResponse.getDescription());

			strResponse = this.parser.createResponse(response);
		}

		return strResponse;
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
