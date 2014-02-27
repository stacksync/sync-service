package com.stacksync.syncservice.storage.swift;

import java.util.List;

public class AccessObject {
	
	private TokenObject token;
	private List<ServiceObject> serviceCatalog;
	
	public TokenObject getToken() {
		return token;
	}
	public List<ServiceObject> getServiceCatalog() {
		return serviceCatalog;
	}
}
