package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.ObjectMetadata;

public class APIDeleteResponse extends APIResponse {

	public APIDeleteResponse(ObjectMetadata object, Boolean success, int error, String description) {
		super(null);
		this.success = success;
		this.errorCode = error;
		this.description = description;
		if (object != null) {
			this.object = new CommitInfo(object.getFileId(), object.getRootId(), object.getVersion(),
					success, object);
		}
	}

}
