package com.stacksync.syncservice.db;

import java.sql.Connection;

import com.stacksync.syncservice.db.postgresql.PostgresqlDeviceDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlObject1DAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlObjectVersionDao;
import com.stacksync.syncservice.db.postgresql.PostgresqlUserDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlWorkspaceDAO;

public class DAOFactory {

	private String type;

	public DAOFactory(String type) {
		this.type = type;
	}

	public WorkspaceDAO getWorkspaceDao(Connection connection) {
		return new PostgresqlWorkspaceDAO(connection);
	}

	public UserDAO getUserDao(Connection connection) {
		return new PostgresqlUserDAO(connection);
	}

	public Object1DAO getObject1DAO(Connection connection) {
		return new PostgresqlObject1DAO(connection);
	}

	public ObjectVersionDAO getObjectVersionDAO(Connection connection) {
		return new PostgresqlObjectVersionDao(connection);
	}

	public DeviceDAO getDeviceDAO(Connection connection) {
		return new PostgresqlDeviceDAO(connection);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
