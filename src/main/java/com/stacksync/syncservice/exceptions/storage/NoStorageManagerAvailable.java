package com.stacksync.syncservice.exceptions.storage;

public class NoStorageManagerAvailable extends Exception {

	private static final long serialVersionUID = -2162586363263343293L;

	public NoStorageManagerAvailable(String message) {
		super(message);
	}
}
