package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;

import java.util.UUID;

@Distributed(key = "id")
public class CommitInfoRMI {

   public UUID id;
   private Long committedVersion;
   private boolean commitSucceed;
   private ItemMetadataRMI metadata;

   @Deprecated
   public CommitInfoRMI(){}

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

}
