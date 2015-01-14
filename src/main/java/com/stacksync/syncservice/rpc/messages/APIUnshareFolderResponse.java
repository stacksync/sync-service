package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

public class APIUnshareFolderResponse extends APIResponse {
	private List<User> usersToRemove;
	private Workspace workspace;
	private boolean isUnshared;

	public APIUnshareFolderResponse(Workspace workspace,  List<User> usersToRemove, boolean isUnshared, Boolean success, int error, String description) {
		super();

		this.success = success;
		this.workspace = workspace;
		this.description = description;
		this.errorCode = error;
		this.usersToRemove = usersToRemove;
		this.isUnshared = isUnshared;
	}

	public Workspace getWorkspace() {
		return workspace;
	}
	
	public List<User> getUsersToRemove() {
		return usersToRemove;
	}

	public boolean isUnshared() {
		return isUnshared;
	}

	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {

			JsonArray sharedTo = new JsonArray();

			for (User user : usersToRemove) {
				JsonObject jUser = parseUser(user);
				sharedTo.add(jUser);
			}

			jResponse.add("unshared_to", sharedTo);

		} else {
			jResponse.addProperty("error", getErrorCode());
			jResponse.addProperty("description", getDescription());
		}

		return jResponse.toString();
	}
}
