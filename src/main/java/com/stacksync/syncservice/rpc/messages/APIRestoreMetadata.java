package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.ItemMetadata;

public class APIRestoreMetadata extends APIResponse {

	public APIRestoreMetadata(ItemMetadata item, Boolean success, int error, String description) {
		super(null);
		this.success = success;
		this.errorCode = error;
		this.description = description;
		this.item = new CommitInfo(item.getVersion(),
				success, item);
	}
}
