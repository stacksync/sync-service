package com.stacksync.syncservice.db.infinispan.models;

import java.io.Serializable;
import java.util.UUID;

public class CommitInfoRMI implements Serializable{

   private UUID id;
   private Long committedVersion;
   private boolean commitSucceed;
   private ItemMetadataRMI metadata;

   public CommitInfoRMI(Long committedVersion, boolean commitSucceed,ItemMetadataRMI metadata) {
      this.id = UUID.randomUUID();
      this.committedVersion = committedVersion;
      this.commitSucceed = commitSucceed;
      this.metadata = metadata;

   }

   public long getCommittedVersion() {
      return committedVersion;
   }

   public void setCommittedVersion(Long committedVersion) {
      this.committedVersion = committedVersion;
   }

   public boolean isCommitSucceed() {
      return commitSucceed;
   }

   public void setCommitSucceed(boolean commitSucceed) {
      this.commitSucceed = commitSucceed;
   }

   public ItemMetadataRMI getMetadata() {
      return metadata;
   }

   public void setMetadata(ItemMetadataRMI metadata) {
      this.metadata = metadata;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      CommitInfoRMI that = (CommitInfoRMI) o;

      return id.equals(that.id);

   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }
}
