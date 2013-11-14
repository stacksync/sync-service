package com.stacksync.syncservice.exceptions;

public class CommitWrongVersionNoParent extends Exception{

	private static final long serialVersionUID = -5529999497610319369L;

	public CommitWrongVersionNoParent() {
		super();
	}

	public CommitWrongVersionNoParent(String message) {
		super(message);
	}

	public CommitWrongVersionNoParent(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitWrongVersionNoParent(Throwable cause) {
		super(cause);
	}
	
}
