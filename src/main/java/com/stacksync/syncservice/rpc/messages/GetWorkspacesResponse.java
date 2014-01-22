package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.stacksync.syncservice.model.Workspace;

public class GetWorkspacesResponse extends APIResponse {

	private List<Workspace> workspaces;
	private boolean succed;
	private String description;

	public GetWorkspacesResponse(String requestId, List<Workspace> workspaces, boolean succed, String description) {
		super(requestId);
		this.workspaces = workspaces;
		this.succed = succed;
		this.description = description;
	}

	public String getResult() {
		return succed ? "ok" : "error";
	}

	public String getDescription() {
		return description;
	}

	public List<Workspace> getWorkspaces() {
		return workspaces;
	}

}