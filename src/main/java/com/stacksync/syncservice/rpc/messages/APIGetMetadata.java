package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.ItemMetadata;

public class APIGetMetadata extends APIResponse {

	private ItemMetadata itemMetadata;
	
	public APIGetMetadata(ItemMetadata item, Boolean success, int error, String description) {
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
