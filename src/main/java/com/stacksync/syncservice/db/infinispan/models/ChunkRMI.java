package com.stacksync.syncservice.db.infinispan.models;

import java.io.Serializable;
import java.util.UUID;

public class ChunkRMI implements Serializable{

   public UUID uuid;
   private Integer order = null;
   private String clientChunkName = null;

   @Deprecated
   public ChunkRMI() {
      uuid = UUID.randomUUID();
   }

   public ChunkRMI(String name, Integer order) {
      this.uuid = UUID.randomUUID();
      this.clientChunkName = name;
      this.order = order;
   }

   public Integer getOrder() {
      return order;
   }

   public void setOrder(Integer order) {
      this.order = order;
   }

   public String getClientChunkName() {
      return clientChunkName;
   }

   public void setClientChunkName(String clientChunkName) {
      this.clientChunkName = clientChunkName;
   }

   public boolean isValid() {
      //TODO: Unimplemented method
      return true;
   }

   @Override
   public String toString() {
      String format = "Chunk[clientChunkName=%s, order=%s]";
      String result = String.format(format, clientChunkName, order);

      return result;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      ChunkRMI chunkRMI = (ChunkRMI) o;

      return uuid.equals(chunkRMI.uuid);

   }

   @Override
   public int hashCode() {
      return uuid.hashCode();
   }
}
