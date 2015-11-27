package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.Connection;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.commons.api.BasicCache;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanConnection implements Connection {

   private AtomicObjectFactory factory;
   private BasicCache basicCache;

   public InfinispanConnection(BasicCache cache) {
      this.factory = AtomicObjectFactory.forCache(cache);
      this.basicCache = cache;
   }

   public AtomicObjectFactory getFactory() {
      return this.factory;
   }

   @Override
   public void setAutoCommit(boolean autoCommit) throws Exception { }

   @Override
   public void commit() throws Exception { }

   @Override
   public void rollback() throws Exception { }

   @Override
   public void close() throws Exception {
      this.factory.close();
   }

   public void cleanup() {
      basicCache.clear();
   }
}
