package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.Connection;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.client.hotrod.RemoteCache;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanConnection implements Connection {
    
    private AtomicObjectFactory factory;
    
    public InfinispanConnection(RemoteCache cache) {
    //public InfinispanConnection(Cache cache) {
        this.factory = new AtomicObjectFactory(cache);
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
    public void close() throws Exception { }
    
    
    
}
