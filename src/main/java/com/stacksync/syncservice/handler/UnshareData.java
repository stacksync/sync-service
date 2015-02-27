package com.stacksync.syncservice.handler;


import java.util.List;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;

public class UnshareData {
	private List<UserRMI> usersToRemove;
	private WorkspaceRMI workspace;
	private boolean isUnshared;
	
	public UnshareData(List<UserRMI> usersToRemove, WorkspaceRMI workspace,
			boolean isUnshared) {
		this.usersToRemove = usersToRemove;
		this.workspace = workspace;
		this.isUnshared = isUnshared;
	}

	public List<UserRMI> getUsersToRemove() {
		return usersToRemove;
	}

	public void setUsersToRemove(List<UserRMI> usersToRemove) {
		this.usersToRemove = usersToRemove;
	}

	public WorkspaceRMI getWorkspace() {
		return workspace;
	}

	public void setWorkspace(WorkspaceRMI workspace) {
		this.workspace = workspace;
	}

	public boolean isUnshared() {
		return isUnshared;
	}

	public void setUnshared(boolean isUnshared) {
		this.isUnshared = isUnshared;
	}
	
}
