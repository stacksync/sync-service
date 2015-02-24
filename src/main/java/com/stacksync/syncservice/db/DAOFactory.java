package com.stacksync.syncservice.db;

import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.InfinispanDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlDeviceDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlItemDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlItemVersionDao;
import com.stacksync.syncservice.db.postgresql.PostgresqlUserDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlWorkspaceDAO;
import org.infinispan.atomic.AtomicObjectFactory;

public class DAOFactory {

	private String type;
        private InfinispanDAO infinispanGenericDAO;

	public DAOFactory(String type) {
		this.type = type;
	}
        
        private void createInfinispanDAO(InfinispanConnection connection) {
            AtomicObjectFactory factory = connection.getFactory();
            this.infinispanGenericDAO = new InfinispanDAO(factory);
        }

	public WorkspaceDAO getWorkspaceDao(Connection connection) {
            if (type.equalsIgnoreCase("postgresql")) {
                return new PostgresqlWorkspaceDAO(connection);
            } else if (type.equalsIgnoreCase("infinispan")) {
                if (this.infinispanGenericDAO == null) {
                    createInfinispanDAO((InfinispanConnection) connection);
                }
                //return this.infinispanGenericDAO;
                return null;
            }
            
            return null;
	}

	public UserDAO getUserDao(Connection connection) {
		return new PostgresqlUserDAO(connection);
	}

	public ItemDAO getItemDAO(Connection connection) {
		return new PostgresqlItemDAO(connection);
	}

	public ItemVersionDAO getItemVersionDAO(Connection connection) {
		return new PostgresqlItemVersionDao(connection);
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
