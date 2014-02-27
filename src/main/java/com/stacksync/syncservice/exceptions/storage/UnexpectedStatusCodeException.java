package com.stacksync.syncservice.exceptions.storage;

public class UnexpectedStatusCodeException extends Exception {

	private static final long serialVersionUID = 2336051403508530828L;

	public UnexpectedStatusCodeException(String message) {
		super(message);
	}
}
