package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;

import java.util.UUID;

@Distributed(key="userId")
public class AccountInfoRMI {

   public UUID userId;

   private String name;
   private String email;
   private Integer quotaLimit;
   private Integer quotaUsed;
   private String swiftTenant;
   private String swiftUser;
   private String swiftAuthUrl;

   @Deprecated
   public AccountInfoRMI(){}

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

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      AccountInfoRMI that = (AccountInfoRMI) o;

      return userId.equals(that.userId);

   }

   @Override public int hashCode() {
      return userId.hashCode();
   }
}
