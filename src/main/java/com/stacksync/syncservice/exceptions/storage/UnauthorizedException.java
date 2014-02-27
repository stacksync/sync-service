package com.stacksync.syncservice.exceptions.storage;

public class UnauthorizedException extends Exception {

	private static final long serialVersionUID = 37491715204419935L;

	public UnauthorizedException(String message) {
		super(message);
	}
	
}
