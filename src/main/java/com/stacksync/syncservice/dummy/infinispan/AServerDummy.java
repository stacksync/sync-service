/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.CommitInfo;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.InfinispanDeviceDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanUserDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanWorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.HashMap;
import org.infinispan.util.concurrent.locks.DeadlockDetectedException;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public abstract class AServerDummy extends Thread {

    protected final Logger logger = Logger.getLogger(AServerDummy.class.getName());

    protected static final int CHUNK_SIZE = 512 * 1024;

    private int numberOfItems;
    protected int commitsPerMinute, minutes;

    protected InfinispanConnection connection;
    protected Handler handler;
    protected InfinispanUserDAO userDAO;
    protected InfinispanDeviceDAO deviceDAO;
    protected InfinispanWorkspaceDAO workspaceDAO;

    protected HashMap<Long, Long> ids;
    protected HashMap<Long, String> filenames;

    public AServerDummy(ConnectionPool pool, int commitsPerMinute, int minutes) throws SQLException, NoStorageManagerAvailable, Exception {
        this.connection = (InfinispanConnection) pool.getConnection();
        this.handler = new SQLSyncHandler(pool);
        
        this.commitsPerMinute = commitsPerMinute;
        this.minutes = minutes;

        DAOFactory factory = new DAOFactory("infinispan");
        this.userDAO = factory.getUserDao(connection);
        this.deviceDAO = factory.getDeviceDAO(connection);
        this.workspaceDAO = factory.getWorkspaceDao(connection);

        this.ids = new HashMap<Long, Long>();
        this.filenames = new HashMap<Long, String>();
    }

    /**
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    public void doCommit(UUID uuid, Action action) throws Exception {
        // Create user info
        UserRMI user = new UserRMI(uuid);
        DeviceRMI device = new DeviceRMI(uuid);
        WorkspaceRMI workspace = new WorkspaceRMI(uuid);

        // Create a ItemMetadata List
        List<ItemMetadata> items = new ArrayList<ItemMetadata>();

        Long tempId = action.getTempId();
        if (!filenames.containsKey(tempId)){
            filenames.put(tempId, java.util.UUID.randomUUID().toString());
        }
                
        ItemMetadata itemMetadata = action.createItemMetadata(uuid, ids.get(tempId), filenames.get(tempId));
        items.add(itemMetadata);

        logger.info("hander_doCommit_start,commitID=" + tempId);
        List<CommitInfo> commitInfo = handler.doCommit(user, workspace, device, items);
        logger.info("hander_doCommit_end,commitID=" + tempId);
        Long metadataId = commitInfo.get(0).getMetadata().getId();

        if (!commitInfo.get(0).isCommitSucceed()) {
            System.out.println("buuu");
        }

        if (!ids.containsKey(itemMetadata.getTempId())) {
            ids.put(itemMetadata.getTempId(), metadataId);
        }
    }

    public void setup(UUID uuid) throws RemoteException {

        DeviceRMI device = new DeviceRMI(uuid);

        WorkspaceRMI workspace = new WorkspaceRMI(uuid);
        workspace.addUser(uuid);
        workspace.setOwner(uuid);

        UserRMI user = new UserRMI(uuid);
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

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems += numberOfItems;
    }

    public int getNumberOfItems() {
        return this.numberOfItems;
    }

}
