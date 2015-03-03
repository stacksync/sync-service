package com.stacksync.syncservice.db;

import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.InfinispanDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanDeviceDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanItemDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanItemVersionDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanUserDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanWorkspaceDAO;
import org.infinispan.atomic.AtomicObjectFactory;

public class DAOFactory {

        private static DAOFactory instance = null;
	private String type;
        private InfinispanDAO infinispanGenericDAO;

	private DAOFactory(String type) {
		this.type = type;
	}
        
        public static DAOFactory getInstance(){
            if (instance == null) {
                instance = new DAOFactory("infinispan");
            }
            return instance;
        }
        
        private void createInfinispanDAO(InfinispanConnection connection) {
            AtomicObjectFactory factory = connection.getFactory();
            this.infinispanGenericDAO = new InfinispanDAO(factory);
        }

	public InfinispanWorkspaceDAO getWorkspaceDao(Connection connection) {
            if (this.infinispanGenericDAO == null) {
                    createInfinispanDAO((InfinispanConnection) connection);
                }
                return this.infinispanGenericDAO;
	}

	public InfinispanUserDAO getUserDao(Connection connection) {
		if (this.infinispanGenericDAO == null) {
                    createInfinispanDAO((InfinispanConnection) connection);
                }
                return this.infinispanGenericDAO;
	}

	public InfinispanItemDAO getItemDAO(Connection connection) {
		if (this.infinispanGenericDAO == null) {
                    createInfinispanDAO((InfinispanConnection) connection);
                }
                return this.infinispanGenericDAO;
	}

	public InfinispanItemVersionDAO getItemVersionDAO(Connection connection) {
		if (this.infinispanGenericDAO == null) {
                    createInfinispanDAO((InfinispanConnection) connection);
                }
                return this.infinispanGenericDAO;
	}

	public InfinispanDeviceDAO getDeviceDAO(Connection connection) {
		if (this.infinispanGenericDAO == null) {
                    createInfinispanDAO((InfinispanConnection) connection);
                }
                return this.infinispanGenericDAO;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
