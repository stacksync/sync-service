package com.stacksync.syncservice.rpc.messages;


public class GetWorkspaces extends APIResponse {

	private String userCloudId;
	private String responseQueue;

	public GetWorkspaces(String requestId, String userCloudId, String responseQueue) {
		super(requestId);
		this.userCloudId = userCloudId;
		this.responseQueue = responseQueue;
	}

	public String getUserCloudId() {
		return userCloudId;
	}

	public void setUserCloudId(String userCloudId) {
		this.userCloudId = userCloudId;
	}

	public String getResponseQueue() {
		return responseQueue;
	}

	public void setResponseQueue(String responseQueue) {
		this.responseQueue = responseQueue;
	}

}
