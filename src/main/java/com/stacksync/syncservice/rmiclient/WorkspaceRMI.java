package com.stacksync.syncservice.rmiclient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkspaceRMI implements Serializable {

	private static final long serialVersionUID = 243350300638953723L;

	private UUID id;
	private String name;
	private ItemRMI parentItem;
	private Integer latestRevision;
	private UserRMI owner;
	private String swiftContainer;
	private String swiftUrl;
	private boolean isShared;
	private boolean isEncrypted;
	private List<ItemRMI> items;
	private List<UserRMI> users;

	public WorkspaceRMI() {
		this(null);
	}

	public WorkspaceRMI(UUID id) {
		this(id, 0, null, false, false);
	}

	public WorkspaceRMI(UUID id, Integer latestRevision, UserRMI owner, boolean isShared, boolean isEncrypted ) {
		this.id = id;
		this.latestRevision = latestRevision;
		this.owner = owner;
		this.isShared = isShared;
		this.isEncrypted = isEncrypted;
		this.items = new ArrayList<ItemRMI>();
		this.users = new ArrayList<UserRMI>();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Integer getLatestRevision() {
		return latestRevision;
	}

	public void setLatestRevision(Integer latestRevision) {
		this.latestRevision = latestRevision;
	}

	public UserRMI getOwner() {
		return owner;
	}

	public void setOwner(UserRMI owner) {
		this.owner = owner;
	}
	
	public boolean isShared() {
		return isShared;
	}

	public boolean isEncrypted() {
		return isEncrypted;
	}
	
	public void setEncrypted(boolean isEncrypted){
		this.isEncrypted = isEncrypted;
	}
	
	public void setShared(Boolean isShared) {
		this.isShared =isShared;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSwiftContainer() {
		return swiftContainer;
	}

	public void setSwiftContainer(String swiftContainer) {
		this.swiftContainer = swiftContainer;
	}
	
	public String getSwiftUrl() {
		return swiftUrl;
	}

	public void setSwiftUrl(String swiftUrl) {
		this.swiftUrl = swiftUrl;
	}

	public ItemRMI getParentItem() {
		return parentItem;
	}

	public void setParentItem(ItemRMI parentItem) {
		this.parentItem = parentItem;
	}

	public List<ItemRMI> getItems() {
		return items;
	}

	public void setItems(List<ItemRMI> items) {
		this.items = items;
	}

	public void addItem(ItemRMI item) {
		this.items.add(item);
	}

	public void removeObject(ItemRMI object) {
		this.items.remove(object);
	}

	public List<UserRMI> getUsers() {
		return users;
	}

	public void setUsers(List<UserRMI> users) {
		this.users = users;
	}

	public void addUser(UserRMI user) {
		this.users.add(user);
	}

	public void removeUser(UserRMI user) {
		this.users.remove(user);
	}

	/**
	 * Checks whether the user contains all required attributes (ID is not
	 * required since it is assigned automatically when a user is inserted to
	 * the database)
	 * 
	 * @return Boolean True if the user is valid. False otherwise.
	 */
	public boolean isValid() {
		if (this.owner == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String toString() {
		return String.format(
				"workspace[id=%s, latestRevision=%s, owner=%s, items=%s, users=%s]", id,
				latestRevision, owner, items, users);
	}
}