package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.commons.models.UserWorkspace;

public class APIGetFolderMembersResponse extends APIResponse {

	private List<UserWorkspace> members;

	public APIGetFolderMembersResponse(List<UserWorkspace> members, Boolean success, int error, String description) {
		super();

		this.success = success;
		this.members = members;
		this.description = description;
		this.errorCode = error;
	}

	public List<UserWorkspace> getMembers() {
		return members;
	}

	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {

			JsonArray list = new JsonArray();

			for (UserWorkspace userWorkspace : members) {
				JsonObject jUser = parseUserWorkspace(userWorkspace);
				list.add(jUser);
			}

			return list.toString();

		} else {
			jResponse.addProperty("error", getErrorCode());
			jResponse.addProperty("description", getDescription());
			return jResponse.toString();
		}

		
	}
}
