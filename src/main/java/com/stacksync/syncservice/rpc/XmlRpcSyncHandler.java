package com.stacksync.syncservice.rpc;

import java.util.ArrayList;
import java.util.List;

import omq.common.broker.Broker;
import omq.exception.RemoteException;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.omq.RemoteWorkspace;
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
	private Handler handler;
	private IParser parser;
	private Broker broker;

	public XmlRpcSyncHandler(Broker broker, ConnectionPool pool) {
		try {
			this.handler = new SQLHandler(pool);
			this.broker = broker;
			this.parser = Reader.getInstance("com.stacksync.syncservice.rpc.parser.JSONParser");

			logger.info("XMLRPC server set up done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getMetadata(String strUser, String strRequestId, String strFileId, String strIncludeList, String strIncludeDeleted, String strIncludeChunks,
			String strVersion) {

		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		Boolean includeList = Boolean.parseBoolean(strIncludeList);
		Boolean includeDeleted = Boolean.parseBoolean(strIncludeDeleted);
		Boolean includeChunks = Boolean.parseBoolean(strIncludeChunks);

		Long version = null;
		try {
			version = Long.parseLong(strVersion);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> get_metadata -->[User:" + strUser + ", Request:" + strRequestId + ", fileId:" + fileId + ", includeList: " + includeList
				+ ", includeDeleted: " + includeDeleted + "," + "version: " + version + "]");

		APIGetMetadata response = this.handler.ApiGetMetadata(strUser, fileId, includeList, includeDeleted, includeChunks, version);
		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String getVersions(String strUser, String strRequestId, String strFileId) {

		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		// TODO: filtrar versiones borradas!!
		logger.debug("XMLRPC -> get_versions -->[User:" + strUser + ", Request:" + strRequestId + ", fileId:" + fileId + "]");

		APIGetVersions response = this.handler.ApiGetVersions(strUser, fileId);
		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	private APIGetMetadata getParentMetadata(String strUser, Long parentId) {
		Long version = null;
		Boolean list = true;
		Boolean includeDeleted = true;
		Boolean includeChunks = false;

		APIGetMetadata metadataResponse = this.handler.ApiGetMetadata(strUser, parentId, list, includeDeleted, includeChunks, version);

		return metadataResponse;
	}

	private String checkParentMetadata(Long parentId, APIGetMetadata metadataResponse) {
		String strResponse = "";
		ObjectMetadata parentMetadata = metadataResponse.getObjectMetadata();

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
			APICommitResponse response = new APICommitResponse(null, metadataResponse.getSuccess(), metadataResponse.getErrorCode(),
					metadataResponse.getDescription());

			strResponse = this.parser.createResponse(response);
		}

		return strResponse;
	}

	public String putMetadataFile(String strUser, String strRequestId, String strFileName, String strParentId, String strOverwrite, String strChecksum,
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

		Boolean overwrite = true;
		if (strOverwrite.length() > 0) {
			overwrite = Boolean.parseBoolean(strOverwrite);
		}

		logger.debug("XMLRPC -> put_metadata_file -->[User:" + strUser + ", Request:" + strRequestId + ", FileName:" + strFileName + ", parentId: "
				+ strParentId + ", Overwrite: " + overwrite + ", Checksum: " + checksum + ", Filesize: " + fileSize + ", Mimetype: " + strMimetype
				+ ", Chunks: " + strChunks + "]");

		APIGetMetadata metadataResponse = getParentMetadata(strUser, parentId);
		String strParentResponse = checkParentMetadata(parentId, metadataResponse);

		if (strParentResponse.length() > 0) {// error
			logger.debug("XMLRPC -> Error resp -->[" + strParentResponse + "]");
			return strParentResponse;
		}

		ObjectMetadata parentMetadata = metadataResponse.getObjectMetadata();
		ObjectMetadata object = new ObjectMetadata();

		object.setFileName(strFileName);
		object.setFileSize(fileSize);
		object.setChecksum(checksum);
		object.setMimetype(strMimetype);
		object.setChunks(strChunks);

		String workspace = strUser + "/";
		APICommitResponse response = this.handler.ApiCommitMetadata(strUser, workspace, overwrite, object, parentMetadata);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, strRequestId, response);
		}

		String strResponse = this.parser.createResponse(response);

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String deleteMetadataFile(String strUser, String strRequestId, String strFileId) {
		Long fileId = null;
		try {
			fileId = Long.parseLong(strFileId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> delete_metadata_file -->[User:" + strUser + ", Request:" + strRequestId + ", fileId:" + fileId + "]");

		String workspace = strUser + "/";

		ObjectMetadata object = new ObjectMetadata();
		object.setFileId(fileId);

		APIDeleteResponse response = this.handler.ApiDeleteMetadata(strUser, workspace, object);
		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, strRequestId, response);
		}

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String putMetadataFolder(String strUser, String strRequestId, String strFolderName, String strParentId) {
		Long parentId = null;
		try {
			parentId = Long.parseLong(strParentId);
		} catch (NumberFormatException ex) {
		}

		logger.debug("XMLRPC -> put_metadata_folder -->[User:" + strUser + ", Request:" + strRequestId + ", folderName:" + strFolderName + ", parent: "
				+ strParentId + "]");

		String workspace = strUser + "/";

		APIResponse response;
		if (strFolderName.length() == 0) {
			response = new APICreateFolderResponse(null, false, 400, "Folder name cannot be empty.");
		} else {
			APIGetMetadata metadataResponse = getParentMetadata(strUser, parentId);
			String strParentResponse = checkParentMetadata(parentId, metadataResponse);

			if (strParentResponse.length() > 0) {// error
				logger.debug("XMLRPC -> Error resp -->[" + strParentResponse + "]");
				return strParentResponse;
			}

			ObjectMetadata parentMetadata = metadataResponse.getObjectMetadata();
			ObjectMetadata object = new ObjectMetadata();
			object.setFileName(strFolderName);
			object.setFolder(true);

			response = this.handler.ApiCreateFolder(strUser, workspace, object, parentMetadata);
		}

		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, strRequestId, response);
		}

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	public String restoreMetadata(String strUser, String strRequestId, String strFileId, String strVersion) {
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

		logger.debug("XMLRPC -> restore_file -->[User:" + strUser + ", Request:" + strRequestId + ", fileId:" + strFileId + ", version: " + strVersion + "]");

		String workspace = strUser + "/";

		ObjectMetadata object = new ObjectMetadata();
		object.setFileId(fileId);
		object.setVersion(version);

		APIRestoreMetadata response = this.handler.ApiRestoreMetadata(strUser, workspace, object);
		String strResponse = this.parser.createResponse(response);

		if (response.getSuccess()) {
			this.sendMessageToClients(workspace, strRequestId, response);
		}

		logger.debug("XMLRPC -> resp -->[" + strResponse + "]");
		return strResponse;
	}

	private void sendMessageToClients(String workspaceName, String requestID, APIResponse generalResponse) {
		
		CommitInfo info = generalResponse.getObject();
		List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();
		responseObjects.add(info);
		CommitResult result = new CommitResult(requestID, responseObjects);

		RemoteWorkspace commitNotifier;
		try {
			commitNotifier = broker.lookupMulti(workspaceName, RemoteWorkspace.class);
			commitNotifier.notifyCommit(result);
		} catch (RemoteException e) {
			//e.printStackTrace();
			logger.error("Error sending the notification to the clients: "+e);
		}

	}
}
