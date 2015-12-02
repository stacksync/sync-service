package com.stacksync.syncservice.db;

import com.stacksync.syncservice.db.infinispan.DummyDAO;
import com.stacksync.syncservice.db.infinispan.GlobalDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.InfinispanDAO;

import java.util.UUID;

public class DAOFactory {

   private String type;
   private static UUID uuid = UUID.randomUUID();

   public DAOFactory(String type) {
      this.type = type;
   }

   private static GlobalDAO createDAO(Connection connection, UUID uuid) {

      if (connection instanceof InfinispanConnection){
         return  new InfinispanDAO(uuid);
      }else if (connection instanceof DummyConnection) {
         return  new DummyDAO();
      }

      throw new IllegalArgumentException("invalid connection");


   }

   public GlobalDAO getDAO(Connection connection, UUID uuid) {
      return createDAO(connection, uuid);
   }

   public GlobalDAO getDAO(Connection connection) {
      return createDAO(connection,DAOFactory.uuid);
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }
}
