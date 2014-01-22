package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.stacksync.syncservice.models.ItemMetadata;

public class Commit extends APIResponse {

	private String user;
	private List<ItemMetadata> items;
	private Long deviceId;
	private String workspaceName;

	public Commit(String user, String requestId, List<ItemMetadata> items, Long deviceId, String workspaceName) {
		super(requestId);
		this.items = items;
		this.deviceId = deviceId;
		this.workspaceName = workspaceName;
		this.user = user;
	}

	public List<ItemMetadata> getItems() {
		return this.items;
	}

	public String getWorkspaceName() {
		return this.workspaceName;
	}


	public Long getDeviceId() {
		return this.deviceId;
	}

	public String getUser() {
		return user;
	}

}
