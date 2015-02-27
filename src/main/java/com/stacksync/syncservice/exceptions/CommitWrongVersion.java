package com.stacksync.syncservice.exceptions;

import com.stacksync.syncservice.db.infinispan.models.ItemRMI;

public class CommitWrongVersion extends Exception {

	private static final long serialVersionUID = 632312815010044106L;
	private ItemRMI item;

	public CommitWrongVersion() {
		super();
	}

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
