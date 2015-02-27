package com.stacksync.syncservice.rpc.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;

public class APIGetVersions extends APIResponse {

	private ItemMetadataRMI itemMetadata;
	
	public APIGetVersions(ItemMetadataRMI item, Boolean success, int error, String description) {
		super();

		this.success = success;
		this.itemMetadata = item;
		this.description = description;
		this.errorCode = error;
	}
	
	public ItemMetadataRMI getItemMetadata(){
		return itemMetadata;
	}
	
	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {
			ItemMetadataRMI metadata = getItemMetadata();
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
			jResponse.addProperty("error", getErrorCode());
			jResponse.addProperty("description", getDescription());
		}

		return jResponse.toString();
	}
}
