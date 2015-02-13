package com.stacksync.syncservice.rpc.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

public class APINewUserResponse extends APIResponse{
	private Workspace workspace;
	private Item folder;
	private String user;
	private String pass;
	private Boolean isNewWorkspace;

	public APINewUserResponse(Workspace workspace, Item folder, Boolean success, String name, String pass, boolean isNewWorkspace, int error, String description) {
		super();

		this.success = success;
		this.workspace = workspace;
		this.description = description;
		this.errorCode = error;
		this.user = name;
		this.pass = pass;
		this.isNewWorkspace = isNewWorkspace;
		this.folder = folder;
		
	}

	public Item getFolder() {
		return folder;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public String getUser() {
		return user;
	}

	public String getPass() {
		return pass;
	}

	public Boolean getIsNewWorkspace() {
		return isNewWorkspace;
	}

	@Override
	public String toString() {
		JsonObject jResponse = new JsonObject();

		if (getSuccess()) {
			//return user and password
			JsonArray userInfo = new JsonArray();
			JsonObject jUser = new JsonObject();
			jUser.addProperty("name", user);
			jUser.addProperty("pass", pass);
			userInfo.add(jUser);			

			jResponse.add("user", userInfo);

		} else {
			jResponse.addProperty("error", getErrorCode());
			jResponse.addProperty("description", getDescription());
		}

		return jResponse.toString();
	}
}
