package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.ObjectMetadata;

public class APICommitResponse extends APIResponse {

	private Boolean success;
	private int error;
	private String description;

	public APICommitResponse(ObjectMetadata object, Boolean success, int error, String description) {
		super(null);
		this.success = success;
		this.error = error;
		this.description = description;
		if (object != null) {
			this.object = new CommitInfo(object.getFileId(), object.getRootId(), object.getVersion(),
					success, object);
		}
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public int getErrorCode() {
		return error;
	}

	public void setError(int error) {
		this.error = error;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CommitInfo getObject() {
		return object;
	}

	public void setObject(CommitInfo object) {
		this.object = object;
	}

}
