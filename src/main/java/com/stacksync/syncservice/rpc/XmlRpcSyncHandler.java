package com.stacksync.syncservice.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import com.stacksync.syncservice.handler.SQLAPIHandler;
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

		logger.debug(String.format("XMLRPC Request. getMetadata [userId: %s, fileId: %s, chunks: %s, version: %s]",
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
			String strFileSize, String strMimetype, List<String> strChunks) {

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

		ItemMetadata parentItem = metadataResponse.getItemMetadata();
		ItemMetadata item = new ItemMetadata();

		item.setId(null);
		item.setTempId(new Random().nextLong());
		item.setFilename(strFileName);
		item.setSize(fileSize);
		item.setChecksum(checksum);
		item.setMimetype(strMimetype);
		item.setChunks(strChunks);

		User user = new User();
		user.setId(userId);

		APICommitResponse response = this.apiHandler.createFile(user, item, parentItem);


		if (response.getSuccess()) {
			this.sendMessageToClients(response.getMetadata().getWorkspaceId().toString(), response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String updateData(String strUserId, String strFileId, String strChecksum,
			String strFileSize, String strMimetype, List<String> strChunks) {

		logger.debug("XMLRPC -> update data -->[User:" + strUserId + ", Checksum: "
				+ strChecksum + ", Filesize: " + strFileSize + ", Mimetype: " + strMimetype + ", Chunks: " + strChunks
				+ "]");

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
		item.setChunks(strChunks);

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

	public String deleteItem(String strUserId, String strFileId) {
		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> delete_metadata_file -->[User:" + strUserId + ", fileId:" + fileId + "]");

		ItemMetadata object = new ItemMetadata();
		object.setId(fileId);

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

		APIRestoreMetadata response = this.apiHandler.ApiRestoreMetadata(user, object);
		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, response);
		}

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
