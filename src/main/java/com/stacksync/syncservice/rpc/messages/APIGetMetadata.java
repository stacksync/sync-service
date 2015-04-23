package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ExternalFolderMetadata;


public class APIGetMetadata extends APIResponse {

	private ItemMetadata itemMetadata;
	private List<ExternalFolderMetadata> externalFolderMetadata;
	
	public APIGetMetadata(ItemMetadata item, Boolean success, int error, String description, List<ExternalFolderMetadata> externalFolderMetadata) {
		super();

		this.success = success;
		this.itemMetadata = item;
		this.externalFolderMetadata = externalFolderMetadata;
		this.description = description;
		this.errorCode = error;
	}
	
	public ItemMetadata getItemMetadata(){
		return itemMetadata;
	}
	
	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {
			ItemMetadata metadata = getItemMetadata();
			jResponse = parseObjectMetadataForAPI(metadata);

			if (metadata.getChildren() != null) {
				JsonArray contents = new JsonArray();

				for (ItemMetadata entry : metadata.getChildren()) {
					JsonObject entryJson = parseObjectMetadataForAPI(entry);
					contents.add(entryJson);
				}
				if (externalFolderMetadata != null){
					for (ExternalFolderMetadata externalFolder : externalFolderMetadata){
						JsonObject entryJson = parseExternalMetadata(externalFolder);
						contents.add(entryJson);
					}
				}

				jResponse.add("contents", contents);
			}
		} else {
			jResponse.addProperty("error", getErrorCode());
			jResponse.addProperty("description", getDescription());
		}
		
		return jResponse.toString();
	}
}
