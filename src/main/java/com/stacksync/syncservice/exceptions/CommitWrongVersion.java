package com.stacksync.syncservice.exceptions;

import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class CommitWrongVersion extends DAOException {

	private ItemRMI item;

	public CommitWrongVersion(String message) {
		super(message);
	}
	
	public CommitWrongVersion(String message, ItemRMI item) {
		super(message);
		this.item = item;
	}

	public CommitWrongVersion(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitWrongVersion(Throwable cause) {
		super(cause);
	}
	
	public ItemRMI getItem() {
		return item;
	}

}
