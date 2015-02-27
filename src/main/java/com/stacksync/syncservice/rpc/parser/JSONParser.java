package com.stacksync.syncservice.rpc.parser;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;

public class JSONParser implements IParser {

	private static final Logger logger = Logger.getLogger(JSONParser.class.getName());

	public JSONParser() { }

	public String createResponse(APIResponse response) {
		JsonObject jResponse = null;
		String bResponse = "";

		try {
			if (response instanceof APIGetMetadata) {
				jResponse = this.createGetMetadataResponse((APIGetMetadata) response);
			} else if (response instanceof APIGetVersions) {
				jResponse = this.createGetVersionsResponse((APIGetVersions) response);
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
			ItemMetadataRMI metadata = response.getItemMetadata();
			jResponse = parseObjectMetadataForAPI(metadata);

			if (metadata.getChildren() != null) {
				JsonArray contents = new JsonArray();

				for (ItemMetadataRMI entry : metadata.getChildren()) {
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
		JsonObject jResponse;

		if (!response.getSuccess()) {
			jResponse = new JsonObject();
			jResponse.addProperty("description", response.getDescription());
			jResponse.addProperty("error", response.getErrorCode());
		} else {
			ItemMetadataRMI file = response.getItem().getMetadata();
			jResponse = this.parseItemMetadata(file);
		}

		return jResponse;
	}

	private JsonObject createGetMetadataResponse(APIGetMetadata response) {
		JsonObject jResponse = new JsonObject();

		if (response.getSuccess()) {
			ItemMetadataRMI metadata = response.getItemMetadata();
			jResponse = parseObjectMetadataForAPI(metadata);

			if (metadata.getChildren() != null) {
				JsonArray contents = new JsonArray();

				for (ItemMetadataRMI entry : metadata.getChildren()) {
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

	private JsonObject parseMetadata(ItemMetadataRMI metadata) {
		JsonObject jMetadata = new JsonObject();

		if (metadata == null) {
			return jMetadata;
		}

		jMetadata.addProperty("id", metadata.getId());
		jMetadata.addProperty("parent_id", metadata.getParentId());
		jMetadata.addProperty("filename", metadata.getFilename());
		jMetadata.addProperty("is_folder", metadata.isFolder());
		jMetadata.addProperty("status", metadata.getStatus());

		if (metadata.getModifiedAt() != null) {
			jMetadata.addProperty("modified_at", metadata.getModifiedAt().toString());
		}		

		jMetadata.addProperty("version", metadata.getVersion());
		jMetadata.addProperty("checksum", metadata.getChecksum());
		jMetadata.addProperty("size", metadata.getSize());
		jMetadata.addProperty("mimetype", metadata.getMimetype());

		return jMetadata;
	}

	private JsonObject parseObjectMetadataForAPI(ItemMetadataRMI metadata) {
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

	private JsonObject parseItemMetadata(ItemMetadataRMI metadata) {
		JsonObject jMetadata = parseMetadata(metadata);

		if (metadata.getParentId() == null) {
			jMetadata.addProperty("parent_file_id", "");
		} else {
			jMetadata.addProperty("parent_file_id", metadata.getParentId());
		}

		if (metadata.getParentId() == null) {
			jMetadata.addProperty("parent_file_version", "");
		} else {
			jMetadata.addProperty("parent_file_version", metadata.getParentVersion());
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
