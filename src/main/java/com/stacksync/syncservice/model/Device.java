package com.stacksync.syncservice.model;

public class Device {

	private Long id;
	private String name;
	private User user;

	public Device() {
		this.id = null;
	}

	public Device(Long id, String name, User user) {
		this.id = id;
		this.name = name;
		this.user = user;
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}

}
