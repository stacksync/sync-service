package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

import java.util.UUID;

@Distributed
public class ChunkRMI {

   @Key
   public UUID uuid;
   private Integer order = null;
   private String clientChunkName = null;

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
}
