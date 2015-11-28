/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.test.dummy;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.DeviceDAO;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public abstract class AServerDummy extends Thread {

    protected final Logger logger = Logger.getLogger(AServerDummy.class.getName());

    protected static final int CHUNK_SIZE = 512 * 1024;

    protected int commitsPerMinute, minutes;
    protected InfinispanConnection connection;
    protected Handler handler;
    private UserDAO userDAO;
    private DeviceDAO deviceDAO;
    private WorkspaceDAO workspaceDAO;

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

    public void doCommit(UUID uuid, Random ran, int min, int max, String id) throws DAOException {
        // Create user info
        UserRMI user = new UserRMI(uuid);
        DeviceRMI device = new DeviceRMI(uuid,"android",user);
        WorkspaceRMI workspace = new WorkspaceRMI(uuid);

        // Create a ItemMetadata List
        List<ItemMetadataRMI> items = new ArrayList<>();
        items.add(createItemMetadata(ran, min, max, uuid));

        logger.info("hander_doCommit_start,commitID=" + id);
        handler.doCommit(user, workspace, device, items);
        logger.info("hander_doCommit_end,commitID=" + id);
    }

    private ItemMetadataRMI createItemMetadata(Random ran, int min, int max, UUID deviceId) {
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

        ItemMetadataRMI itemMetadata = new ItemMetadataRMI(id, version, deviceId, parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);
        itemMetadata.setChunks(chunks);
        itemMetadata.setTempId((long) ran.nextInt(10));

        return itemMetadata;
    }

    private String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

    }

    public void setup(UUID uuid) throws RemoteException {
        /*try {
            String[] create = new String[]{
                "INSERT INTO user1 (id, name, swift_user, swift_account, email, quota_limit) VALUES ('" + id + "', '" + id + "', '"
                + id + "', '" + id + "', '" + id + "@asdf.asdf', 0);",
                "INSERT INTO workspace (id, latest_revision, owner_id, is_shared, swift_container, swift_url) VALUES ('" + id
                + "', 0, '" + id + "', false, '" + id + "', 'STORAGEURL');",
                "INSERT INTO workspace_user(workspace_id, user_id, workspace_name, parent_item_id) VALUES ('" + id + "', '" + id
                + "', 'default', NULL);",
                "INSERT INTO device (id, name, user_id, os, app_version) VALUES ('" + id + "', '" + id + "', '" + id + "', 'LINUX', 1)"};

            Statement statement;

            statement = connection.createStatement();

            for (String query : create) {
                statement.executeUpdate(query);
            }

            statement.close();
        } catch (SQLException e) {
            logger.error(e);
        }*/
        
        /*DeviceRMI device = new DeviceRMI(deviceID);
        
        WorkspaceRMI workspace = new WorkspaceRMI(workspaceID);
        workspace.addUser(userID);
        workspace.setOwner(userID);
        
        UserRMI user = new UserRMI(userID);
        user.setEmail("a");
        user.setName("a");
        user.setQuotaLimit(10);
        user.setQuotaUsed(0);
        user.setSwiftAccount("a");
        user.setSwiftUser("a");
        user.addDevice(device);
        user.addWorkspace(workspaceID);
        userDAO.add(user);
        
        workspaceDAO.add(workspace);
        deviceDAO.add(device);*/
        
        WorkspaceRMI workspace = new WorkspaceRMI(uuid, 0, new UserRMI(uuid),false,false);

        UserRMI user = new UserRMI(uuid);
        DeviceRMI device = new DeviceRMI(uuid,"",user);
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

}
