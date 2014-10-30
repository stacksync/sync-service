package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.User;

public class APIUserMetadata extends APIResponse {
	
	private User user;

	public APIUserMetadata(User user, Boolean success, int error, String description) {
		super();
		this.success = success;
		this.description = description;
		this.errorCode = error;
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}

}
