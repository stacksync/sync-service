package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;

public class APIDeleteResponse extends APIResponse {

	public APIDeleteResponse(ItemMetadata item, Boolean success, int error, String description) {
		super(null);
		this.success = success;
		this.errorCode = error;
		this.description = description;
		if (item != null) {
			this.item = new CommitInfo(item.getVersion(),
					success, item);
		}
	}

}
