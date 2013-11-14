package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.stacksync.syncservice.models.ObjectMetadata;

public class GetChangesResponse extends APIResponse {

	private Boolean succed;
	private List<ObjectMetadata> metadata;
	private String description;

	public GetChangesResponse(String requestId, Boolean succed, List<ObjectMetadata> metadata, String description) {
		super(requestId);

		this.succed = succed;
		this.metadata = metadata;
		this.description = description;
	}

	public String getResult() {
		return succed ? "ok" : "error";
	}

	public String getDescription() {
		return description;
	}

	public List<ObjectMetadata> getListMetadata() {
		return metadata;
	}

}
