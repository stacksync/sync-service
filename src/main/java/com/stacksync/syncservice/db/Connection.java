package com.stacksync.syncservice.db;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface Connection {
    void setAutoCommit(boolean autoCommit) throws Exception;
    void commit() throws Exception;
    void rollback() throws Exception;
    void close() throws Exception;
    void cleanup() throws Exception;
}
