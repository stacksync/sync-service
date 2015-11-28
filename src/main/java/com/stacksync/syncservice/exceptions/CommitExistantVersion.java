package com.stacksync.syncservice.exceptions;

import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class CommitExistantVersion extends DAOException {

	private ItemRMI item;
	private long version;

	public CommitExistantVersion(String message) {
		super(message);
	}
	
	public CommitExistantVersion(String message, ItemRMI item, long version) {
		super(message);
		this.item = item;
		this.version = version;
	}

	public CommitExistantVersion(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitExistantVersion(Throwable cause) {
		super(cause);
	}
	
	public ItemRMI getItem() {
		return item;
	}

	public long getVersion() {
		return version;
	}

}
