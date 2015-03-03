package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.infinispan.models.CommitInfoRMI;

public class APIDeleteResponse extends APIResponse {

	public APIDeleteResponse(ItemMetadata item, Boolean success, int error, String description) {
		super();
		this.success = success;
		this.errorCode = error;
		this.description = description;
		if (item != null) {
			this.item = new CommitInfoRMI(item.getVersion(),
					success, item);
		}
	}

}
