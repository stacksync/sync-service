package com.stacksync.syncservice.exceptions;

public class InternalServerError extends Exception {

	private static final long serialVersionUID = 7240966151447816363L;

	public InternalServerError() {
		super();
	}

	public InternalServerError(String message) {
		super(message);
	}

	public InternalServerError(String message, Throwable cause) {
		super(message, cause);
	}

	public InternalServerError(Throwable cause) {
		super(cause);
	}
	
}
