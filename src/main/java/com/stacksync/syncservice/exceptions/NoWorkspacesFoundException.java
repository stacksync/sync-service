package com.stacksync.syncservice.exceptions;

public class NoWorkspacesFoundException extends Exception {

	private static final long serialVersionUID = 22874643493599763L;

	public NoWorkspacesFoundException() {
		super();
	}

	public NoWorkspacesFoundException(String message) {
		super(message);
	}

	public NoWorkspacesFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoWorkspacesFoundException(Throwable cause) {
		super(cause);
	}

}
