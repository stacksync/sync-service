package com.stacksync.syncservice.exceptions;

import com.stacksync.syncservice.model.Object1;

public class CommitExistantVersion extends Exception {

	private static final long serialVersionUID = 3790965344729620693L;
	
	private Object1 object;
	private long version;

	public CommitExistantVersion() {
		super();
	}

	public CommitExistantVersion(String message) {
		super(message);
	}
	
	public CommitExistantVersion(String message, Object1 object, long version) {
		super(message);
		this.object = object;
		this.version = version;
	}

	public CommitExistantVersion(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitExistantVersion(Throwable cause) {
		super(cause);
	}
	
	public Object1 getObject() {
		return object;
	}

	public long getVersion() {
		return version;
	}

}
