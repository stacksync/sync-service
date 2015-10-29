package com.stacksync.syncservice.db;

/**
 * @author Pierre Sutra
 */
public class DummyConnectionPool extends ConnectionPool {

   private static DummyConnection instance = new DummyConnection();

   @Override
   public Connection getConnection() throws Exception {
      return instance;
   }
}
