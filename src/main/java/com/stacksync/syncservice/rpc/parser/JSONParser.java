package com.stacksync.syncservice.rpc.parser;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.CommitResponse;
import com.stacksync.syncservice.rpc.messages.GetChangesResponse;
import com.stacksync.syncservice.rpc.messages.GetWorkspacesResponse;

public class JSONParser implements IParser {

	private static final Logger logger = Logger.getLogger(JSONParser.class
			.getName());

	public JSONParser() {
	}

	public String createResponse(APIResponse response) {
		JsonObject jResponse = null;
		String bResponse = "";

		try {
			if (response instanceof CommitResponse) {
				jResponse = this
						.createCommitResponse((CommitResponse) response);
			} else if (response instanceof GetWorkspacesResponse) {
				jResponse = this
						.createGetWorkspacesResponse((GetWorkspacesResponse) response);
			} else if (response instanceof GetChangesResponse) {
				jResponse = this
						.createGetChangesResponse((GetChangesResponse) response);
			} else if (response instanceof APIGetMetadata) {
				jResponse = this
						.createGetMetadataResponse((APIGetMetadata) response);
			} else if (response instanceof APIGetVersions) {
				jResponse = this
						.createGetVersionsResponse((APIGetVersions) response);
			} else if (response instanceof APICommitResponse
					|| response instanceof APIDeleteResponse
					|| response instanceof APIRestoreMetadata
					|| response instanceof APICreateFolderResponse) {
				jResponse = this.createGenericAPIResponse(response);
			}

			if (jResponse != null) {
				bResponse = jResponse.toString();
			}

		} catch (Exception ex) {
			logger.error(ex.toString(), ex);
		}

		return bResponse;
	}

	private JsonObject createGetVersionsResponse(APIGetVersions response) {
		JsonObject jResponse = new JsonObject();

		if (response.getSuccess()) {
			ObjectMetadata metadata = response.getObjectMetadata();
			jResponse = parseObjectMetadataForAPI(metadata);

			if (metadata.getContent() != null) {
				JsonArray contents = new JsonArray();

				for (ObjectMetadata entry : metadata.getContent()) {
					JsonObject entryJson = parseObjectMetadataForAPI(entry);
					contents.add(entryJson);
				}

				jResponse.add("versions", contents);
			}
		} else {
			jResponse.addProperty("error", response.getErrorCode());
			jResponse.addProperty("description", response.getDescription());
		}

		return jResponse;
	}

	private JsonObject createGenericAPIResponse(APIResponse response) {
		JsonObject jResponse = new JsonObject();
		jResponse.addProperty("description", response.getDescription());

		if (!response.getSuccess()) {
			jResponse.addProperty("error", response.getErrorCode());
		} else {
			ObjectMetadata file = response.getObject().getMetadata();
			JsonObject metadata = this.parseObjectMetadata(file);
			jResponse.add("metadata", metadata);
		}

		return jResponse;
	}

	private JsonObject createGetWorkspacesResponse(
			GetWorkspacesResponse response) {
		JsonObject jResponse = new JsonObject();

		jResponse.addProperty("requestId", response.getRequestId());
		jResponse.addProperty("type", response.getClass().getSimpleName());
		jResponse.addProperty("result", response.getResult());
		jResponse.addProperty("description", response.getDescription());

		JsonArray jWorkspaces = new JsonArray();
		List<Workspace> workspaces = response.getWorkspaces();
		for (Workspace w : workspaces) {
			JsonObject jWorkspace = new JsonObject();

			jWorkspace.addProperty("id", w.getClientWorkspaceName());
			jWorkspace.addProperty("latestRevision", w.getLatestRevision());
			// TODO kitar??
			jWorkspace.addProperty("path", "/");

			jWorkspaces.add(jWorkspace);
		}
		jResponse.add("workspaces", jWorkspaces);

		return jResponse;
	}

	private JsonObject createCommitResponse(CommitResponse response) {
		JsonObject jResponse = new JsonObject();

		jResponse.addProperty("requestId", response.getRequestId());
		jResponse.addProperty("type", response.getClass().getSimpleName());

		JsonArray jFiles = new JsonArray();
		List<CommitInfo> files = response.getObjects();
		for (CommitInfo file : files) {

			JsonObject jFile = new JsonObject();
			jFile.addProperty("file_id", file.getFileId());
			jFile.addProperty("root_id", file.getRootId());
			jFile.addProperty("version", file.getVersion());
			jFile.addProperty("committed", file.isCommitted());

			JsonObject metadata = this.parseObjectMetadata(file.getMetadata());
			jFile.add("metadata", metadata);

			jFiles.add(jFile);
		}

		jResponse.add("files", jFiles);
		return jResponse;
	}

	private JsonObject createGetChangesResponse(GetChangesResponse response) {
		JsonObject jResponse = new JsonObject();

		jResponse.addProperty("requestId", response.getRequestId());
		jResponse.addProperty("type", response.getClass().getSimpleName());
		jResponse.addProperty("result", response.getResult());
		jResponse.addProperty("description", response.getDescription());

		JsonArray jMetadata = new JsonArray();
		List<ObjectMetadata> files = response.getListMetadata();

		for (ObjectMetadata file : files) {
			JsonObject metadata = this.parseObjectMetadata(file);
			jMetadata.add(metadata);
		}

		jResponse.add("metadata", jMetadata);

		return jResponse;
	}

	private JsonObject createGetMetadataResponse(APIGetMetadata response) {
		JsonObject jResponse = new JsonObject();

		if (response.getSuccess()) {
			ObjectMetadata metadata = response.getObjectMetadata();
			jResponse = parseObjectMetadataForAPI(metadata);

			if (metadata.getContent() != null) {
				JsonArray contents = new JsonArray();

				for (ObjectMetadata entry : metadata.getContent()) {
					JsonObject entryJson = parseObjectMetadataForAPI(entry);
					contents.add(entryJson);
				}

				jResponse.add("contents", contents);
			}
		} else {
			jResponse.addProperty("error", response.getErrorCode());
			jResponse.addProperty("description", response.getDescription());
		}

		return jResponse;
	}

	private JsonObject parseMetadata(ObjectMetadata metadata) {
		JsonObject jMetadata = new JsonObject();

		if (metadata == null) {
			return jMetadata;
		}

		jMetadata.addProperty("file_id", metadata.getFileId());
		jMetadata.addProperty("parent_file_id", metadata.getParentFileId());
		jMetadata.addProperty("filename", metadata.getFileName());
		jMetadata.addProperty("path", metadata.getFilePath());
		jMetadata.addProperty("is_folder", metadata.isFolder());
		jMetadata.addProperty("status", metadata.getStatus());

		if (metadata.getServerDateModified() != null) {
			jMetadata.addProperty("server_modified", metadata
					.getServerDateModified().toString());
		}

		if (metadata.getServerDateModified() != null) {
			jMetadata.addProperty("client_modified", metadata
					.getClientDateModified().toString());
		}

		jMetadata.addProperty("user", metadata.getClientName());
		jMetadata.addProperty("version", metadata.getVersion());
		jMetadata.addProperty("checksum", metadata.getChecksum());
		jMetadata.addProperty("size", metadata.getFileSize());
		jMetadata.addProperty("mimetype", metadata.getMimetype());

		return jMetadata;
	}

	private JsonObject parseObjectMetadataForAPI(ObjectMetadata metadata) {
		JsonObject jMetadata = parseMetadata(metadata);

		if (metadata.isFolder()) {
			jMetadata.addProperty("is_root", metadata.isRoot());
		} else {
			JsonArray chunks = new JsonArray();
					
			for (String chunk : metadata.getChunks()) {
				JsonElement elem = new JsonPrimitive(chunk);
				chunks.add(elem);
			}			
			
			jMetadata.add("chunks", chunks);
		}

		return jMetadata;
	}

	private JsonObject parseObjectMetadata(ObjectMetadata metadata) {
		JsonObject jMetadata = parseMetadata(metadata);

		jMetadata.addProperty("root_id", metadata.getRootId());
		if (metadata.getParentFileId() == null) {
			jMetadata.addProperty("parent_file_id", "");
		} else {
			jMetadata.addProperty("parent_file_id", metadata.getParentFileId());
		}

		if (metadata.getParentFileId() == null) {
			jMetadata.addProperty("parent_file_version", "");
		} else {
			jMetadata.addProperty("parent_file_version",
					metadata.getParentFileVersion());
		}

		// TODO: send only chunks when is a file
		if (!metadata.isFolder()) {
			JsonArray chunks = new JsonArray();
			for (String chunk : metadata.getChunks()) {
				JsonElement elem = new JsonPrimitive(chunk);
				chunks.add(elem);
			}
			jMetadata.add("chunks", chunks);
		}

		return jMetadata;
	}

}
