package com.stacksync.syncservice.rpc.messages;

import com.stacksync.syncservice.models.ObjectMetadata;

public class APIGetVersions extends APIResponse {

	private ObjectMetadata objectMetadata;
	
	public APIGetVersions(ObjectMetadata object, Boolean success, int error, String description) {
		super(null);

		this.success = success;
		this.objectMetadata = object;
		this.description = description;
		this.errorCode = error;
	}
	
	public ObjectMetadata getObjectMetadata(){
		return objectMetadata;
	}
}
