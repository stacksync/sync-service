package com.stacksync.syncservice.exceptions;

import com.stacksync.syncservice.model.Object1;

public class CommitWrongVersion extends Exception {

	private static final long serialVersionUID = 632312815010044106L;
	private Object1 object;

	public CommitWrongVersion() {
		super();
	}

	public CommitWrongVersion(String message) {
		super(message);
	}
	
	public CommitWrongVersion(String message, Object1 object) {
		super(message);
		this.object = object;
	}

	public CommitWrongVersion(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitWrongVersion(Throwable cause) {
		super(cause);
	}
	
	public Object1 getObject() {
		return object;
	}

}
