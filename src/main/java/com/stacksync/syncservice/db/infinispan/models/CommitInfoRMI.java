package com.stacksync.syncservice.db.infinispan.models;

import java.io.Serializable;
import java.util.UUID;

// @Distributed
public class CommitInfoRMI implements Serializable {

   private static final long serialVersionUID = -1205107021066864318L;

   // @Key
   public UUID id;
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

}
