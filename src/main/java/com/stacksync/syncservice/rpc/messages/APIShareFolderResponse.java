package com.stacksync.syncservice.rpc.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

public class APIShareFolderResponse extends APIResponse {

	private Workspace workspace;

	public APIShareFolderResponse(Workspace workspace, Boolean success, int error, String description) {
		super();

		this.success = success;
		this.workspace = workspace;
		this.description = description;
		this.errorCode = error;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {

			JsonArray sharedTo = new JsonArray();

			for (User user : workspace.getUsers()) {
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
