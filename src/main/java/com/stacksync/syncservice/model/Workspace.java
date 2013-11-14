package com.stacksync.syncservice.model;

import java.util.ArrayList;
import java.util.List;

public class Workspace {

	private Long id;
	private String clientWorkspaceName;
	private Integer latestRevision;
	private User owner;
	private List<Object1> objects;
	private List<User> users;

	public Workspace() {
		this(null);
	}

	public Workspace(Long id) {
		this(id, null, null, null);
	}

	public Workspace(Long id, String clientWorkspaceName, Integer latestRevision, User owner) {
		this.id = id;
		this.clientWorkspaceName = clientWorkspaceName;
		this.latestRevision = latestRevision;
		this.owner = owner;
		this.objects = new ArrayList<Object1>();
		this.users = new ArrayList<User>();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getClientWorkspaceName() {
		return clientWorkspaceName;
	}

	public void setClientWorkspaceName(String clientWorkspaceName) {
		this.clientWorkspaceName = clientWorkspaceName;
	}

	public Integer getLatestRevision() {
		return latestRevision;
	}

	public void setLatestRevision(Integer latestRevision) {
		this.latestRevision = latestRevision;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public List<Object1> getObjects() {
		return objects;
	}

	public void setObjects(List<Object1> objects) {
		this.objects = objects;
	}

	public void addObject(Object1 object) {
		this.objects.add(object);
	}

	public void removeObject(Object1 object) {
		this.objects.remove(object);
	}

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public void addUser(User user) {
		this.users.add(user);
	}

	public void removeUser(User user) {
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
		if (this.clientWorkspaceName == null || this.owner == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String toString() {
		return String.format(
				"workspace[id=%d, clientWorkspaceName=%s, latestRevision=%s, owner=%s, objects=%s, users=%s]", id,
				clientWorkspaceName, latestRevision, owner, objects, users);
	}
}
