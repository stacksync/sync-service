/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.DeviceDAO;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class AOFUtil {

    private UserDAO userDAO;
    private DeviceDAO deviceDAO;
    private WorkspaceDAO workspaceDAO;
    private InfinispanConnection connection;

    public AOFUtil(ConnectionPool pool) throws Exception {
        this.connection = (InfinispanConnection) pool.getConnection();
        DAOFactory factory = new DAOFactory("infinispan");
        this.userDAO = factory.getDAO(connection);
        this.deviceDAO = factory.getDAO(connection);
        this.workspaceDAO = factory.getDAO(connection);
    }

    public void setup(UserRMI user) throws RemoteException {

        UUID uuid = user.getId();
        System.out.println("New workspace: " + uuid.toString());
        WorkspaceRMI workspace = new WorkspaceRMI(uuid,0,user,false,false);

        DeviceRMI device = new DeviceRMI(uuid,"android",user);
        user.setEmail(uuid.toString());
        user.setName("a");
        user.setQuotaLimit(10);
        user.setQuotaUsed(0);
        user.setSwiftAccount("a");
        user.setSwiftUser("a");
        //user.addDevice(device);
        user.addWorkspace(uuid);
        userDAO.add(user);

        workspaceDAO.add(workspace);
        deviceDAO.add(device);
    }

    public InfinispanConnection getConnection() {
        return this.connection;
    }
}
