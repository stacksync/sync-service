package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.ObjectMetadata;

public class APIRestoreMetadata extends APIResponse {

	public APIRestoreMetadata(ObjectMetadata object, Boolean success, int error, String description) {
		super(null);
		this.success = success;
		this.errorCode = error;
		this.description = description;
		this.object = new CommitInfo(object.getFileId(), object.getRootId(), object.getVersion(),
				success, object);
	}
}
