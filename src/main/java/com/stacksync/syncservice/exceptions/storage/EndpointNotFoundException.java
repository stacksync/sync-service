package com.stacksync.syncservice.exceptions.storage;

public class EndpointNotFoundException extends Exception {

	private static final long serialVersionUID = -2739684204840352075L;
	
	public EndpointNotFoundException(String message) {
		super(message);
	}

	public EndpointNotFoundException() {
		super();
	}

}
