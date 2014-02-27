package com.stacksync.syncservice.storage.swift;

public class TokenObject {
	
	private String issued_at;
	private String expires;
	private String id;
	
	public String getIssuedAt() {
		return issued_at;
	}
	public String getExpires() {
		return expires;
	}
	public String getId() {
		return id;
	}
	
	
}
