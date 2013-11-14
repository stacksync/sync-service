package com.stacksync.syncservice.exceptions;

public class NotEnoughConsumersException extends Exception {

	private static final long serialVersionUID = -3470994628999546970L;

	public NotEnoughConsumersException() {
		super();
	}

	public NotEnoughConsumersException(String message) {
		super(message);
	}

	public NotEnoughConsumersException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotEnoughConsumersException(Throwable cause) {
		super(cause);
	}

}
