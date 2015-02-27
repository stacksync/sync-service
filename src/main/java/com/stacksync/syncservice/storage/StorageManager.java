package com.stacksync.syncservice.storage;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;

public abstract class StorageManager {
	
	public enum StorageType {
	    SWIFT, FTP 
	}
	
	public abstract void login() throws Exception; 
	
	public abstract void createNewWorkspace(WorkspaceRMI workspace) throws Exception;
	
	public abstract void removeUserToWorkspace(UserRMI owner, UserRMI user, WorkspaceRMI workspace) throws Exception;
	
	public abstract void grantUserToWorkspace(UserRMI owner, UserRMI user, WorkspaceRMI workspace) throws Exception;
	
	public abstract void copyChunk(WorkspaceRMI sourceWorkspace, WorkspaceRMI destinationWorkspace, String chunkName) throws Exception;
	
	public abstract void deleteWorkspace(WorkspaceRMI workspace) throws Exception;
}
