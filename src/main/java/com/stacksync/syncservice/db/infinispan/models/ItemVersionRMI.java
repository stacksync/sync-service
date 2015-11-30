package com.stacksync.syncservice.db.infinispan.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ItemVersionRMI implements Serializable{

   public Long id;
   private Long itemId;
   private UUID deviceId;
   private Long version;
   private Date committedAt;
   private Date modifiedAt;
   private Long checksum;
   private String status;
   private Long size;
   private List<ChunkRMI> chunks;

   @Deprecated
   public ItemVersionRMI(){}

   public ItemVersionRMI(Long id, Long itemId, UUID deviceId, Long version, Date committedAt,
         Date modifiedAt, Long checksum, String status, Long size) {
      this.id = id;
      this.itemId = itemId;
      this.deviceId = deviceId;
      this.version = version;
      this.committedAt = committedAt;
      this.modifiedAt = modifiedAt;
      this.checksum = checksum;
      this.status = status;
      this.size = size;
      this.chunks = new ArrayList<>();
   }

   public ItemVersionRMI(ItemMetadataRMI metadata, UserRMI userRMI) {
      if (metadata==null) return; // FIXME
      this.id = metadata.getVersion();
      this.itemId = metadata.getParentId();
      this.deviceId = metadata.getDeviceId();
      this.version = metadata.getVersion();
      this.modifiedAt = metadata.getModifiedAt();
      this.checksum = metadata.getChecksum();
      this.status = metadata.getStatus();
      this.size = metadata.getSize();
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getItemId() {
      return itemId;
   }

   public void setItem(Long itemId) {
      this.itemId = itemId;
   }

   public UUID getDeviceId() {
      return deviceId;
   }

   public void setDeviceId(UUID deviceId) {
      this.deviceId= deviceId;
   }

   public Long getVersion() {
      return version;
   }

   public void setVersion(Long version) {
      this.version = version;
   }

   public Date getModifiedAt() {
      return modifiedAt;
   }

   public void setModifiedAt(Date modifiedAt) {
      this.modifiedAt = modifiedAt;
   }

   public Long getChecksum() {
      return checksum;
   }

   public void setChecksum(Long checksum) {
      this.checksum = checksum;
   }

   public Date getCommittedAt() {
      return committedAt;
   }

   public void setCommittedAt(Date committedAt) {
      this.committedAt = committedAt;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public Long getSize() {
      return size;
   }

   public void setSize(Long size) {
      this.size = size;
   }

   public List<ChunkRMI> getChunks() {
      return chunks;
   }

   public void setChunks(List<ChunkRMI> chunks) {
      this.chunks = chunks;
   }

   public boolean isValid() {
      // TODO Auto-generated method stub
      return true;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      ItemVersionRMI that = (ItemVersionRMI) o;

      return id.equals(that.id);

   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }

   @Override
   public String toString() {
      String format = "ItemVersion[id=%s, itemId=%s, "
            + "version=%s, chunks=%s, deviceId=%s, modifiedAt=%s, "
            + "committedAt=%s, checksum=%s, status=%s, "
            + "size=%s]";

      Integer chunksSize = null;
      if (chunks != null) {
         chunksSize = chunks.size();
      }

      String result = String.format(format, id, itemId, version, chunksSize, deviceId, modifiedAt,
            committedAt, checksum, status, size);

      return result;
   }

   public void addChunks(List<ChunkRMI> chunks) {
      chunks.addAll(chunks);
   }

   public void createChunks(List<String> chunksString) {
      if (chunksString != null) {
         if (chunksString.size() > 0) {
            List<ChunkRMI> chunks = new ArrayList<>();
            int i = 0;
            for (String chunkName : chunksString) {
               chunks.add(new ChunkRMI(chunkName, i));
               i++;
            }
            addChunks(chunks);
         }
      }
   }

}
