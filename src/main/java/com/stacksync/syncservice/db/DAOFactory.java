package com.stacksync.syncservice.db;

import com.stacksync.syncservice.db.infinispan.*;
import com.stacksync.syncservice.db.infinispan.models.*;
import org.infinispan.atomic.AtomicObjectFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DAOFactory {

    private String type;
    private static Map<Connection,InfinispanDAO> instance  = new HashMap<>();

    public DAOFactory(String type) {
        this.type = type;
    }

    private static synchronized InfinispanDAO createDAO(Connection connection) {

        if (!instance.containsKey(connection)) {

           if (connection instanceof InfinispanConnection){

              AtomicObjectFactory factory = ((InfinispanConnection) connection).getFactory();

              instance.put(
                    connection,
                    new InfinispanDAO(
                          factory.getInstanceOf(HashMap.class,"deviceMap"),
                          factory.getInstanceOf(HashMap.class,"userMap"),
                          factory.getInstanceOf(HashMap.class,"mailMap"),
                          factory.getInstanceOf(HashMap.class,"workspaceMap"),
                          factory.getInstanceOf(HashMap.class,"itemMap"),
                          factory.getInstanceOf(HashMap.class,"itemVersionMap")));

           }else if (connection instanceof DummyConnection) {

              instance.put(
                    connection,
                    new InfinispanDAO(
                          new HashMap<UUID,DeviceRMI>(),
                          new HashMap<UUID,UserRMI>(),
                          new HashMap<UUID,UserRMI>(),
                          new HashMap<UUID,WorkspaceRMI>(),
                          new HashMap<Long,ItemRMI>(),
                          new HashMap<Long,ItemVersionRMI>()));

           }

        }

       return instance.get(connection);

    }

    public InfinispanWorkspaceDAO getWorkspaceDao(Connection connection) {
        return createDAO(connection);
    }

    public InfinispanUserDAO getUserDao(Connection connection) {
        return createDAO(connection);
    }

    public InfinispanItemDAO getItemDAO(Connection connection) {
        return createDAO(connection);
    }

    public InfinispanItemVersionDAO getItemVersionDAO(Connection connection) {
        return createDAO(connection);
    }

    public InfinispanDeviceDAO getDeviceDAO(Connection connection) {
        return createDAO(connection);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
