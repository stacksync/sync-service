package com.stacksync.syncservice.handler;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.Workspace;

public class NewUserData {
	private Workspace workspace;
	private String userName;
	private String pass;
	private Item item;
	private boolean isNewWorkspace;
	
	public NewUserData(Workspace workspace, Item item, String userName, String pass, boolean isNewWorkspace) {
		super();
		this.workspace = workspace;
		this.userName = userName;
		this.pass = pass;
		this.isNewWorkspace = isNewWorkspace;
		this.item = item;
	}

	public boolean isNewWorkspace() {
		return isNewWorkspace;
	}

	public void setNewWorkspace(boolean isNewWorkspace) {
		this.isNewWorkspace = isNewWorkspace;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	public String getUserName() {
		return userName;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}
	
	
	
	

}
