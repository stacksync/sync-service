package com.stacksync.syncservice.rpc.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.util.UUID;

public class APIShareFolderResponse extends APIResponse {

	private WorkspaceRMI workspace;

	public APIShareFolderResponse(WorkspaceRMI workspace, Boolean success, int error, String description) {
		super();

		this.success = success;
		this.workspace = workspace;
		this.description = description;
		this.errorCode = error;
	}

	public WorkspaceRMI getWorkspace() {
		return workspace;
	}

	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {

			JsonArray sharedTo = new JsonArray();

			for (UUID user : workspace.getUsers()) {
				JsonObject jUser = parseUser(user);
				sharedTo.add(jUser);
			}

			jResponse.add("shared_to", sharedTo);

		} else {
			jResponse.addProperty("error", getErrorCode());
			jResponse.addProperty("description", getDescription());
		}

		return jResponse.toString();
	}
}
