package com.stacksync.syncservice.db;

import com.stacksync.syncservice.db.infinispan.DeviceDAO;
import com.stacksync.syncservice.db.infinispan.*;
import com.stacksync.syncservice.db.infinispan.ItemDAO;
import com.stacksync.syncservice.db.infinispan.ItemVersionDAO;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;

import java.util.HashMap;
import java.util.Map;

public class DAOFactory {

    private String type;
    private static Map<Connection,GlobalDAO> instance  = new HashMap<>();

    public DAOFactory(String type) {
        this.type = type;
    }

    private static synchronized GlobalDAO createDAO(Connection connection) {

        if (!instance.containsKey(connection)) {

           if (connection instanceof InfinispanConnection){

              instance.put(
                    connection,
                    new InfinispanDAO());

           }else if (connection instanceof DummyConnection) {

              instance.put(
                    connection,
                    new DummyDAO());

           }

        }

       return instance.get(connection);

    }

    public WorkspaceDAO getWorkspaceDao(Connection connection) {
        return createDAO(connection);
    }

    public UserDAO getUserDao(Connection connection) {
        return createDAO(connection);
    }

    public ItemDAO getItemDAO(Connection connection) {
        return createDAO(connection);
    }

    public ItemVersionDAO getItemVersionDAO(Connection connection) {
        return createDAO(connection);
    }

    public DeviceDAO getDeviceDAO(Connection connection) {
        return createDAO(connection);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
