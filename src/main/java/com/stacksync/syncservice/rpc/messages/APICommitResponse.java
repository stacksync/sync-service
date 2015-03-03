package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.infinispan.models.CommitInfoRMI;

public class APICommitResponse extends APIResponse {

	public APICommitResponse(ItemMetadata item, Boolean success, int error, String description) {
		this.success = success;
		this.errorCode = error;
		this.description = description;
		if (item != null) {
			this.item = new CommitInfoRMI(item.getVersion(),
					success, item);
		}
	}

}
