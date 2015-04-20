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
import java.util.Date;
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
import java.rmi.RemoteException;

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

    public AServerDummy(ConnectionPool pool, int commitsPerMinute, int minutes) throws SQLException, NoStorageManagerAvailable, Exception {
        this.connection = (InfinispanConnection)pool.getConnection();
        this.commitsPerMinute = commitsPerMinute;
        this.minutes = minutes;
        this.handler = new SQLSyncHandler(pool);
        
        DAOFactory factory = new DAOFactory("infinispan");
        this.userDAO = factory.getUserDao(connection);
        this.deviceDAO = factory.getDeviceDAO(connection);
        this.workspaceDAO = factory.getWorkspaceDao(connection);
    }

    /**
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    public void doCommit(UUID uuid, Random ran, int min, int max, String id) throws Exception {
        // Create user info
        UserRMI user = new UserRMI(uuid);
        DeviceRMI device = new DeviceRMI(uuid);
        WorkspaceRMI workspace = new WorkspaceRMI(uuid);

        // Create a ItemMetadata List
        List<ItemMetadata> items = new ArrayList<ItemMetadata>();
        items.add(createItemMetadata(ran, min, max, uuid));

        logger.info("hander_doCommit_start,commitID=" + id);
        List<CommitInfo> commitInfo = handler.doCommit(user, workspace, device, items);
        logger.info("hander_doCommit_end,commitID=" + id);
    }

    private ItemMetadata createItemMetadata(Random ran, int min, int max, UUID deviceId) {
        String[] mimes = {"pdf", "php", "java", "docx", "html", "png", "jpeg", "xml"};

        Long id = null;
        Long version = 1L;

        Long parentId = null;
        Long parentVersion = null;

        String status = "NEW";
        Date modifiedAt = new Date();
        Long checksum = (long) ran.nextInt(Integer.MAX_VALUE);
        List<String> chunks = new ArrayList<String>();
        Boolean isFolder = false;
        String filename = java.util.UUID.randomUUID().toString();
        String mimetype = mimes[ran.nextInt(mimes.length)];

        // Fill chunks
        int numChunks = ran.nextInt((max - min) + 1) + min;
        long size = numChunks * CHUNK_SIZE;
        for (int i = 0; i < numChunks; i++) {
            String str = java.util.UUID.randomUUID().toString();
            try {
                chunks.add(doHash(str));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        ItemMetadata itemMetadata = new ItemMetadata(id, version, deviceId, parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);
        itemMetadata.setChunks(chunks);
        itemMetadata.setTempId((long) ran.nextLong());

        return itemMetadata;
    }

    private String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

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
