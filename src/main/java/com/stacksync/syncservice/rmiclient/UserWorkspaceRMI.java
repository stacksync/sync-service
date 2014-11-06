package com.stacksync.syncservice.rmiclient;

import java.io.Serializable;
import java.util.Date;

public class UserWorkspaceRMI implements Serializable{

	private static final long serialVersionUID = 7732224675365732811L;
	
	private UserRMI user;
	private WorkspaceRMI workspace;
	private boolean isOwner;
	private Date joinedAt;

	public UserWorkspaceRMI(UserRMI user, WorkspaceRMI workspace) {
		super();
		this.user = user;
		this.workspace = workspace;
	}
	public UserRMI getUser() {
		return user;
	}
	public void setUser(UserRMI user) {
		this.user = user;
	}
	public WorkspaceRMI getWorkspace() {
		return workspace;
	}
	public void setWorkspace(WorkspaceRMI workspace) {
		this.workspace = workspace;
	}
	public boolean isOwner() {
		return isOwner;
	}
	public void setOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}
	public Date getJoinedAt() {
		return joinedAt;
	}
	public void setJoinedAt(Date joinedAt) {
		this.joinedAt = joinedAt;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
}
