package com.stacksync.syncservice.storage;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

public abstract class StorageManager {
	
	public enum StorageType {
	    SWIFT, FTP 
	}
	
	public abstract void login() throws Exception; 
	
	public abstract void createNewWorkspace(User user, Workspace workspace) throws Exception;
	
	public abstract void grantUserToWorkspace(User owner, User user, Workspace workspace) throws Exception;
}
