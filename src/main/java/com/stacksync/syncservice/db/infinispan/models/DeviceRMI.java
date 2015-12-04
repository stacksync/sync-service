package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.ReadOnly;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.UUID;

@Distributed(key="id")
public class DeviceRMI {

   public UUID id;
   public String name;
   public UserRMI user;
   public String os;
   public Date createdAt;
   public Date lastAccessAt;
   public String lastIp;
   public String appVersion;

   @Deprecated
   public DeviceRMI() {}

   public DeviceRMI(UUID id, String name, UserRMI user) {
      this.id = id;
      this.name = name;
      this.user = user;
   }

   public DeviceRMI(String name, UserRMI user, String os, Date createdAt,
         Date lastAccessAt, String lastIp, String appVersion) {
      this.name = name;
      this.user = user;
      this.os = os;
      this.createdAt = createdAt;
      this.lastAccessAt = lastAccessAt;
      this.lastIp = lastIp;
      this.appVersion = appVersion;
   }

   @ReadOnly
   public UUID getId() {
      return id;
   }

   public String getOs() {
      return os;
   }

   public void setOs(String os) {
      this.os = os;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   public Date getLastAccessAt() {
      return lastAccessAt;
   }

   public void setLastAccessAt(Date lastAccessAt) {
      this.lastAccessAt = lastAccessAt;
   }

   public String getLastIp() {
      return lastIp;
   }

   public void setLastIp(String lastIp) {
      this.lastIp = lastIp;
   }

   public String getAppVersion() {
      return appVersion;
   }

   public void setAppVersion(String appVersion) {
      this.appVersion = appVersion;
   }

   public void setId(UUID id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public UserRMI getUser() {
      return user;
   }

   public boolean isValid() {
      // TODO Auto-generated method stub
      return true;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();

      result.append(this.getClass().getName());
      result.append(" {");

      // determine fields declared in this class only (no fields of
      // superclass)
      Field[] fields = this.getClass().getDeclaredFields();

      // print field names paired with their values
      for (Field field : fields) {
         result.append("  ");
         try {
            result.append(field.getName());
            result.append(": ");
            // requires access to private field:
            result.append(field.get(this));
         } catch (IllegalAccessException ex) {
            System.out.println(ex);
         }
      }
      result.append("}");

      return result.toString();
   }

   public boolean belongTo(UserRMI user) {
      return this.user.equals(user);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      DeviceRMI deviceRMI = (DeviceRMI) o;

      return id.equals(deviceRMI.id);

   }

   @Override public int hashCode() {
      return id.hashCode();
   }
}
