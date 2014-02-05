package com.stacksync.syncservice.db;

public enum DAOError {

	// These errors codes correspond to HTTP codes
	// Users
	USER_NOT_FOUND(400, "User not found."), USER_NOT_AUTHORIZED(401,
			"The user is not authorized to access to this resource."),

	// Workspaces
	WORKSPACES_NOT_FOUND(410, "Workspaces not found."),
			
	// Files
	FILE_NOT_FOUND(404, "File or folder not found."),

	// Server
	INTERNAL_SERVER_ERROR(500, "Internal Server Error");

	private final int code;
	private final String message;

	DAOError(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}