package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;

import java.util.Date;
import java.util.UUID;

@Distributed(key = "id")
public class UserWorkspaceRMI {

   public UUID id;
   private UserRMI user;
   private WorkspaceRMI workspace;
   private boolean isOwner;
   private Date joinedAt;

   @Deprecated
   public UserWorkspaceRMI(){}

   public UserWorkspaceRMI(UserRMI user, WorkspaceRMI workspace) {
      super();
      this.id = UUID.randomUUID();
      this.user = user;
      this.workspace = workspace;
   }

   public UserRMI getUser() {
      return user;
   }

   public void setUser(UserRMI user) {
      this.user = user;
   }

   public WorkspaceRMI getWorkspace() {
      return workspace;
   }

   public void setWorkspace(WorkspaceRMI workspace) {
      this.workspace = workspace;
   }

   public boolean isOwner() {
      return isOwner;
   }

   public void setOwner(boolean isOwner) {
      this.isOwner = isOwner;
   }

   public Date getJoinedAt() {
      return joinedAt;
   }

   public void setJoinedAt(Date joinedAt) {
      this.joinedAt = joinedAt;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      UserWorkspaceRMI that = (UserWorkspaceRMI) o;

      return id.equals(that.id);

   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }
}
