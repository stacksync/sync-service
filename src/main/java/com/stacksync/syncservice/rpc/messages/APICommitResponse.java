package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.db.infinispan.models.CommitInfoRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;

public class APICommitResponse extends APIResponse {

	public APICommitResponse(ItemMetadataRMI item, Boolean success, int error, String description) {
		this.success = success;
		this.errorCode = error;
		this.description = description;
		if (item != null) {
			this.item = new CommitInfoRMI(item.getVersion(),
					success, item);
		}
	}

}
