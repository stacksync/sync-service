package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;

public abstract class APIResponse {

	public static String CREATE_FOLDER = "create_folder";
	public static String COMMIT = "commit";
	public static String GET_WORKSPACES = "get_workspaces";
	public static String GET_CHANGES = "get_changes";
	public static String IS_UPDATED = "is_updated";
	public static String GET_METADATA = "get_metadata";
	public static String COMMIT_API = "commit_api";
	public static String DO_DELETE = "do_delete";

	private String requestId;
	protected CommitInfo item;

	protected Boolean success;
	protected int errorCode;
	protected String description;

	public APIResponse(String requestId) {
		this.requestId = requestId;
	}

	public String getRequestId() {
		return requestId;
	}

	public Boolean getSuccess() {
		return success;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getDescription() {
		return description;
	}

	public CommitInfo getItem() {
		return item;
	}
	
	public ItemMetadata getMetadata(){
		return item.getMetadata();
	}

}
