package com.stacksync.syncservice.rpc.messages;

import java.util.List;

import com.stacksync.syncservice.models.ObjectMetadata;

public class Commit extends APIResponse {

	private String user;
	private List<ObjectMetadata> objects;
	private String deviceName;
	private String workspaceName;

	public Commit(String user, String requestId, List<ObjectMetadata> objects, String deviceName, String workspaceName) {
		super(requestId);
		this.objects = objects;
		this.deviceName = deviceName;
		this.workspaceName = workspaceName;
		this.user = user;
	}

	/**
	 * Used for the commit message to get the objects to be updated.
	 * 
	 * @return A list of objects to be committed.
	 * @throws ??
	 */
	public List<ObjectMetadata> getObjects() {
		return this.objects;
	}

	/**
	 * Used for the commit and get_changes messages type.
	 * 
	 * @return The request workspace.
	 * @throws ??
	 */
	public String getWorkspaceName() {
		return this.workspaceName;
	}

	/**
	 * Used for the commit and get_changes messages type.
	 * 
	 * @return The request workspace.
	 * @throws ??
	 */
	public String getDeviceName() {
		return this.deviceName;
	}

	public String getUser() {
		return user;
	}

}
