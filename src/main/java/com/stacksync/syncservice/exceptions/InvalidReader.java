package com.stacksync.syncservice.exceptions;

public class InvalidReader extends Exception {

	private static final long serialVersionUID = 3910376966796379039L;

	public InvalidReader() {
		super();
	}

	public InvalidReader(String message) {
		super(message);
	}

	public InvalidReader(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidReader(Throwable cause) {
		super(cause);
	}
	
}
