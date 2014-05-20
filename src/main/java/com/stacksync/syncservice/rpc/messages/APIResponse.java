package com.stacksync.syncservice.rpc.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;

public abstract class APIResponse {

	public static String CREATE_FOLDER = "create_folder";
	public static String COMMIT = "commit";
	public static String GET_WORKSPACES = "get_workspaces";
	public static String GET_CHANGES = "get_changes";
	public static String IS_UPDATED = "is_updated";
	public static String GET_METADATA = "get_metadata";
	public static String COMMIT_API = "commit_api";
	public static String DO_DELETE = "do_delete";

	protected CommitInfo item;

	protected Boolean success;
	protected int errorCode;
	protected String description;

	public Boolean getSuccess() {
		return success;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getDescription() {
		return description;
	}

	public CommitInfo getItem() {
		return item;
	}
	
	public ItemMetadata getMetadata(){
		return item.getMetadata();
	}
	
	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();
		jResponse.addProperty("description", getDescription());

		if (!getSuccess()) {
			jResponse.addProperty("error", getErrorCode());
		} else {
			ItemMetadata file = getItem().getMetadata();
			JsonObject metadata = this.parseItemMetadata(file);
			jResponse.add("metadata", metadata);
		}

		return jResponse.toString();
	}
	
	private JsonObject parseItemMetadata(ItemMetadata metadata) {
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
	
	protected JsonObject parseObjectMetadataForAPI(ItemMetadata metadata) {
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
	
	protected JsonObject parseMetadata(ItemMetadata metadata) {
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
	

}
