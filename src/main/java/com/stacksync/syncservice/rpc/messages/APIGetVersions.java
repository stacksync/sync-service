package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.ItemMetadata;

public class APIGetVersions extends APIResponse {

	private ItemMetadata itemMetadata;
	
	public APIGetVersions(ItemMetadata item, Boolean success, int error, String description) {
		super(null);

		this.success = success;
		this.itemMetadata = item;
		this.description = description;
		this.errorCode = error;
	}
	
	public ItemMetadata getItemMetadata(){
		return itemMetadata;
	}
}
