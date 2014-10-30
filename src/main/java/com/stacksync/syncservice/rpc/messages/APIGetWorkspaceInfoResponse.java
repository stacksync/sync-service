package com.stacksync.syncservice.rpc.messages;

import com.google.gson.JsonObject;
import com.stacksync.commons.models.Workspace;

public class APIGetWorkspaceInfoResponse extends APIResponse {

	private Workspace workspace;

	public APIGetWorkspaceInfoResponse(Workspace workspace, Boolean success, int error, String description) {
		super();
		this.success = success;
		this.description = description;
		this.errorCode = error;
		this.workspace = workspace;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	@Override
	public String toString() {
		JsonObject jResponse;

		if (!getSuccess()) {
			jResponse = new JsonObject();
			jResponse.addProperty("description", getDescription());
			jResponse.addProperty("error", getErrorCode());
		} else {
			jResponse = this.parseWorkspace();
		}

		return jResponse.toString();
	}

	protected JsonObject parseWorkspace() {

		JsonObject jMetadata = new JsonObject();

		if (workspace == null) {
			return jMetadata;
		}

		jMetadata.addProperty("id", workspace.getId().toString());
		jMetadata.addProperty("name", workspace.getName());
		jMetadata.addProperty("swift_container", workspace.getSwiftContainer());
		jMetadata.addProperty("is_shared", workspace.isShared());
		jMetadata.addProperty("owner", workspace.getOwner().getId().toString());
		jMetadata.addProperty("latest_revision", workspace.getLatestRevision());
		jMetadata.addProperty("parent_item_id", workspace.getParentItem().getId());
		jMetadata.addProperty("is_encrypted", workspace.isEncrypted());

		return jMetadata;
	}

}
