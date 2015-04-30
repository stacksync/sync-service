/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.CommitInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import java.util.HashMap;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public abstract class AReadFile extends Thread {

    protected final Logger logger = Logger.getLogger(AReadFile.class.getName());

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

    public AReadFile(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable, Exception {
        this.connection = (InfinispanConnection) pool.getConnection();
        this.handler = new SQLSyncHandler(pool);

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
        if (!filenames.containsKey(tempId) && action.getStatus().equals("NEW")) {
            filenames.put(tempId, java.util.UUID.randomUUID().toString());
        } else {
            // Trying to do a CHANGED or a DELETED before a NEW from this object
            // or a NEW item with an existing tempId
            System.err.println("Change or Delete before a New or New item with existing tempId");
        }

        boolean correctNewItemVersion = action.getStatus().equals("NEW") && action.version.equals(1L);
        boolean correctChangedDeletedItemVersion = !action.getStatus().equals("NEW") && action.version > 1L;
        ItemMetadata itemMetadata = null;
        if (correctNewItemVersion || correctChangedDeletedItemVersion) {
            // Correct version for the item
            itemMetadata = action.createItemMetadata(uuid, ids.get(tempId), filenames.get(tempId));
            items.add(itemMetadata);
        } else {
            // Incorrect version for the item
            System.err.println("Incorrect version for the item.");
        }

        logger.info("hander_doCommit_start,commitID=" + tempId);
        List<CommitInfo> commitInfo = handler.doCommit(user, workspace, device, items);
        logger.info("hander_doCommit_end,commitID=" + tempId);

        if (commitInfo.isEmpty() || !commitInfo.get(0).isCommitSucceed()) {
            System.err.println("SOME ERROR IN THE COMMIT!!");
        } else {
            System.out.println("__correct__");
            // TODO -> comprovar què passa realment quan es crea un nou item amb el mateix tempId!!!
            Long metadataId = commitInfo.get(0).getMetadata().getId();
            if (!ids.containsKey(itemMetadata.getTempId())) {
                ids.put(itemMetadata.getTempId(), metadataId);
            }
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
