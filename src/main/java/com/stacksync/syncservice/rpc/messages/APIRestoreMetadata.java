package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.db.infinispan.models.CommitInfoRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;

public class APIRestoreMetadata extends APIResponse {

	public APIRestoreMetadata(ItemMetadataRMI item, Boolean success, int error, String description) {
		super();
		this.success = success;
		this.errorCode = error;
		this.description = description;
		this.item = new CommitInfoRMI(item.getVersion(),
				success, item);
	}
}
