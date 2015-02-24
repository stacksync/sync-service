package com.stacksync.syncservice.db;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface Connection {
    
    public void setAutoCommit(boolean autoCommit) throws Exception;
    
    public void commit() throws Exception;
    
    public void rollback() throws Exception;
    
    public void close() throws Exception ;
    
}
