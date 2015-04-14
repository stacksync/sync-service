package com.stacksync.syncservice.storage;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

public abstract class StorageManager {
	
	public enum StorageType {
	    SWIFT, SWIFT_SSL, FTP 
	}
	
	public abstract void login() throws Exception; 
	
	public abstract void createNewWorkspace(Workspace workspace) throws Exception;
	
	public abstract void removeUserToWorkspace(User owner, User user, Workspace workspace) throws Exception;
	
	public abstract void grantUserToWorkspace(User owner, User user, Workspace workspace) throws Exception;
	
	public abstract void copyChunk(Workspace sourceWorkspace, Workspace destinationWorkspace, String chunkName) throws Exception;
	
	public abstract void deleteWorkspace(Workspace workspace) throws Exception;
}
