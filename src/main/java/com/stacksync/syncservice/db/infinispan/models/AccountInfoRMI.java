package com.stacksync.syncservice.db.infinispan.models;

import java.io.Serializable;
import java.util.UUID;
import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

@Distributed
public class AccountInfoRMI implements Serializable{

	private static final long serialVersionUID = 5231686716350716264L;
        
        @Key
	private UUID userId;
        
	private String name;
	private String email;
	private Integer quotaLimit;
	private Integer quotaUsed;
	private String swiftTenant;
	private String swiftUser;
	private String swiftAuthUrl;
	
	public AccountInfoRMI(UUID userId, String name, String email, Integer quotaLimit, Integer quotaUsed, String swiftTenant,
			String swiftUser, String swiftAuthUrl) {
		super();
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.quotaLimit = quotaLimit;
		this.quotaUsed = quotaUsed;
		this.swiftTenant = swiftTenant;
		this.swiftUser = swiftUser;
		this.swiftAuthUrl = swiftAuthUrl;
	}
	
	public AccountInfoRMI(){
		
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getSwiftTenant() {
		return swiftTenant;
	}

	public void setSwiftTenant(String swiftTenant) {
		this.swiftTenant = swiftTenant;
	}

	public String getSwiftUser() {
		return swiftUser;
	}

	public void setSwiftUser(String swiftUser) {
		this.swiftUser = swiftUser;
	}

	public String getSwiftAuthUrl() {
		return swiftAuthUrl;
	}

	public void setSwiftAuthUrl(String swiftAuthUrl) {
		this.swiftAuthUrl = swiftAuthUrl;
	} 
	
	
	
	
}
