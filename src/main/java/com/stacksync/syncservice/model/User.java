package com.stacksync.syncservice.model;

import java.util.ArrayList;
import java.util.List;

public class User {

	private Long id;
	private String name;
	private String cloudId;
	private String email;
	private Integer quotaLimit;
	private Integer quotaUsed;
	private List<Device> devices;
	private List<Workspace> workspaces;

	public User() {
		this(null);
	}

	public User(Long id) {
		this(id, null, null, null, null, null);
	}

	public User(Long id, String name, String cloudId, String email, Integer quotaLimit, Integer quotaUsed) {
		this.id = id;
		this.name = name;
		this.cloudId = cloudId;
		this.email = email;
		this.quotaLimit = quotaLimit;
		this.quotaUsed = quotaUsed;
		this.devices = new ArrayList<Device>();
		this.workspaces = new ArrayList<Workspace>();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCloudId() {
		return cloudId;
	}

	public void setCloudId(String cloudId) {
		this.cloudId = cloudId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getQuotaLimit() {
		return quotaLimit;
	}

	public void setQuotaLimit(Integer quotaLimit) {
		this.quotaLimit = quotaLimit;
	}

	public Integer getQuotaUsed() {
		return quotaUsed;
	}

	public void setQuotaUsed(Integer quotaUsed) {
		this.quotaUsed = quotaUsed;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}

	public void addDevice(Device device) {
		this.devices.add(device);
	}

	public void removeDevice(Device device) {
		this.devices.remove(device);
	}

	public List<Workspace> getWorkspaces() {
		return workspaces;
	}

	public void setWorkspaces(List<Workspace> workspaces) {
		this.workspaces = workspaces;
	}

	public void addWorkspace(Workspace workspace) {
		this.workspaces.add(workspace);
	}

	public void removeWorkspace(Workspace workspace) {
		this.workspaces.remove(workspace);
	}

	@Override
	public boolean equals(Object obj) {
		// if the two objects are equal in reference, they are equal
		if (this == obj) {
			return true;
		} else if (obj instanceof User) {
			User user = (User) obj;
			if (((user.getId() == null) && (this.getId() == null))
					|| user.getId().equals(this.getId())
					&& ((user.getName() == null) && (this.getName() == null) || user.getName().equals(this.getName()))
					&& ((user.getCloudId() == null) && (this.getCloudId() == null) || user.getCloudId().equals(
							this.getCloudId()))
					&& ((user.getEmail() == null) && (this.getEmail() == null) || user.getEmail().equals(
							this.getEmail()))
					&& ((user.getQuotaLimit() == null) && (this.getQuotaLimit() == null) || user.getQuotaLimit()
							.equals(this.getQuotaLimit()))
					&& ((user.getQuotaUsed() == null) && (this.getQuotaUsed() == null) || user.getQuotaUsed().equals(
							this.getQuotaUsed()))) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return String.format("User[id=%d, name=%s, cloudId=%s, email=%s, quotaLimit=%s, quotaUsed=%s]", id, name,
				cloudId, email, quotaLimit, quotaUsed);
	}

	/**
	 * Checks whether the user contains all required attributes (ID is not
	 * required since it is assigned automatically when a user is inserted to
	 * the database)
	 * 
	 * @return Boolean True if the user is valid. False otherwise.
	 */
	public boolean isValid() {
		if (this.cloudId == null || this.email == null || this.name == null || this.quotaLimit == null
				|| this.quotaUsed == null) {
			return false;
		} else {
			return true;
		}
	}

}
