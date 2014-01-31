package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;

public class APICommitResponse extends APIResponse {

	private Boolean success;
	private int error;
	private String description;

	public APICommitResponse(ItemMetadata item, Boolean success, int error, String description) {
		super(null);
		this.success = success;
		this.error = error;
		this.description = description;
		if (item != null) {
			this.item = new CommitInfo(item.getVersion(),
					success, item);
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

	public CommitInfo getItem() {
		return item;
	}

	public void setObject(CommitInfo item) {
		this.item = item;
	}

}
