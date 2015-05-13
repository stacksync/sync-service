/**
 * 
 */
package com.stacksync.syncservice.dummy;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class UserContainer {
	private User user;
	private Workspace workspace;
	private Device device;

	public UserContainer(User user, Workspace workspace, Device device) {
		super();
		this.user = user;
		this.workspace = workspace;
		this.device = device;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

}
