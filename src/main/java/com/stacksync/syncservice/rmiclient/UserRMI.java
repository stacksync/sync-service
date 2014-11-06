package com.stacksync.syncservice.rmiclient;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRMI implements Serializable, Remote {

	private static final long serialVersionUID = -8827608629982195900L;

	private UUID id;
	private String name;
	private String swiftUser;
	private String swiftAccount;
	private String email;
	private Integer quotaLimit;
	private Integer quotaUsed;
	private List<DeviceRMI> devices;
	private List<WorkspaceRMI> workspaces;

	public UserRMI() {
		this(null);
	}

	public UserRMI(UUID id) {
		this(id, null, null, null, null, null, null);
	}

	public UserRMI(UUID id, String name, String swiftUser, String swiftAccount, String email, Integer quotaLimit, Integer quotaUsed) {
		this.id = id;
		this.name = name;
		this.swiftUser = swiftUser;
		this.swiftAccount = swiftAccount;
		this.email = email;
		this.quotaLimit = quotaLimit;
		this.quotaUsed = quotaUsed;
		this.devices = new ArrayList<DeviceRMI>();
		this.workspaces = new ArrayList<WorkspaceRMI>();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSwiftUser() {
		return swiftUser;
	}

	public void setSwiftUser(String swiftUser) {
		this.swiftUser = swiftUser;
	}
	
	public String getSwiftAccount() {
		return swiftAccount;
	}

	public void setSwiftAccount (String swiftAccount) {
		this.swiftAccount = swiftAccount;
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

	public List<DeviceRMI> getDevices() {
		return devices;
	}

	public void setDevices(List<DeviceRMI> devices) {
		this.devices = devices;
	}

	public void addDevice(DeviceRMI device) {
		this.devices.add(device);
	}

	public void removeDevice(DeviceRMI device) {
		this.devices.remove(device);
	}

	public List<WorkspaceRMI> getWorkspaces() {
		return workspaces;
	}

	public void setWorkspaces(List<WorkspaceRMI> workspaces) {
		this.workspaces = workspaces;
	}

	public void addWorkspace(WorkspaceRMI workspace) {
		this.workspaces.add(workspace);
	}

	public void removeWorkspace(WorkspaceRMI workspace) {
		this.workspaces.remove(workspace);
	}

	@Override
	public boolean equals(Object obj) {
		// if the two objects are equal in reference, they are equal
		if (this == obj) {
			return true;
		} else if (obj instanceof UserRMI) {
			UserRMI user = (UserRMI) obj;
			if (((user.getId() == null) && (this.getId() == null))
					|| user.getId().equals(this.getId())
					&& ((user.getName() == null) && (this.getName() == null) || user.getName().equals(this.getName()))
					&& ((user.getSwiftUser() == null) && (this.getSwiftUser() == null) || user.getSwiftUser().equals(
							this.getSwiftUser()))
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
		return String.format("User[id=%s, name=%s, swiftUser=%s, swiftAccount=%s, email=%s, quotaLimit=%s, quotaUsed=%s]", id, name,
				swiftUser, swiftAccount, email, quotaLimit, quotaUsed);
	}

	/**
	 * Checks whether the user contains all required attributes (ID is not
	 * required since it is assigned automatically when a user is inserted to
	 * the database)
	 * 
	 * @return Boolean True if the user is valid. False otherwise.
	 */
	public boolean isValid() {
		if (this.swiftUser == null || this.email == null || this.name == null || this.quotaLimit == null
				|| this.quotaUsed == null) {
			return false;
		} else {
			return true;
		}
	}

}