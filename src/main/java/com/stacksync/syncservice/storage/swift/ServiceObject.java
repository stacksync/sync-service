package com.stacksync.syncservice.storage.swift;

import java.util.List;

public class ServiceObject {
	
	private List<EndpointObject> endpoints;
	private String type;
	private String swift;
	
	public List<EndpointObject> getEndpoints() {
		return endpoints;
	}
	public String getType() {
		return type;
	}
	public String getSwift() {
		return swift;
	}
	
	
}
