package com.stacksync.syncservice.handler;


import java.util.List;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

public class UnshareData {
	private List<User> usersToRemove;
	private Workspace workspace;
	private boolean isUnshared;
	
	public UnshareData(List<User> usersToRemove, Workspace workspace,
			boolean isUnshared) {
		this.usersToRemove = usersToRemove;
		this.workspace = workspace;
		this.isUnshared = isUnshared;
	}

	public List<User> getUsersToRemove() {
		return usersToRemove;
	}

	public void setUsersToRemove(List<User> usersToRemove) {
		this.usersToRemove = usersToRemove;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	public boolean isUnshared() {
		return isUnshared;
	}

	public void setUnshared(boolean isUnshared) {
		this.isUnshared = isUnshared;
	}
	
}
