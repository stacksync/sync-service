package com.stacksync.syncservice.db;

public abstract class ConnectionPool {

	public abstract Connection getConnection() throws Exception;

}
