package com.stacksync.syncservice.exceptions;

import com.stacksync.syncservice.exceptions.dao.DAOException;

public class CommitWrongVersionNoParent extends DAOException{

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
