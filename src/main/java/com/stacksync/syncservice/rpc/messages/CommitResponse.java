package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.stacksync.syncservice.models.CommitInfo;

public class CommitResponse extends APIResponse {

	private List<CommitInfo> objects;

	public CommitResponse(String requestId, List<CommitInfo> objects) {
		super(requestId);
		this.objects = objects;
	}

	public void addCommitResponseObject(CommitInfo object) {
		this.objects.add(object);
	}

	public List<CommitInfo> getObjects() {
		return objects;
	}

	public void setObjects(List<CommitInfo> objects) {
		this.objects = objects;
	}

}
