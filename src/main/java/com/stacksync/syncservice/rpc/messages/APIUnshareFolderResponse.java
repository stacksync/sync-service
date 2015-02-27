package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;

public class APIUnshareFolderResponse extends APIResponse {
	private List<UserRMI> usersToRemove;
	private WorkspaceRMI workspace;
	private boolean isUnshared;

	public APIUnshareFolderResponse(WorkspaceRMI workspace,  List<UserRMI> usersToRemove, boolean isUnshared, Boolean success, int error, String description) {
		super();

		this.success = success;
		this.workspace = workspace;
		this.description = description;
		this.errorCode = error;
		this.usersToRemove = usersToRemove;
		this.isUnshared = isUnshared;
	}

	public WorkspaceRMI getWorkspace() {
		return workspace;
	}
	
	public List<UserRMI> getUsersToRemove() {
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

			for (UserRMI user : usersToRemove) {
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
