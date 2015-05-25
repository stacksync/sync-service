/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.sql.SQLException;
import java.util.UUID;

import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class WorkloadFromFileBenchmark extends Thread {

    protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StaticBenchmark.class.getName());

    private int linesSize;
    private long totalTime;

    protected HashMap<Long, UUID> users;
    protected ArrayList<Long> userIds;
    protected ArrayList<Action> lines;

    private int itemsCount;

    protected Handler handler;
    private AOFUtil utils;
    protected HashMap<Long, Long> ids;
    protected HashMap<Long, String> filenames;
    
    protected static final int CHUNK_SIZE = 512 * 1024;

    public WorkloadFromFileBenchmark(ConnectionPool pool, int numUsers, String fileName) throws SQLException,
            NoStorageManagerAvailable,
            Exception {

        this.utils = new AOFUtil(pool);
        this.handler = new SQLSyncHandler(pool);

        this.lines = new ArrayList<Action>();
        BufferedReader read = new BufferedReader(new FileReader(fileName));
        String line = read.readLine();
        String[] lineParts;
        while (line != null) {
            lineParts = line.split(",");
            if (lineParts[1].equals("NEW")) {
                lines.add(new New(lineParts));
            } else if (lineParts[1].equals("CHANGED")) {
                lines.add(new Modify(lineParts));
            } else if (lineParts[1].equals("DELETED")) {
                lines.add(new Delete(lineParts));
            }
        this.ids = new HashMap<Long, Long>();
        this.filenames = new HashMap<Long, String>();
            line = read.readLine();
        }
        read.close();

        linesSize = lines.size();
        users = new HashMap<Long, UUID>();
        userIds = new ArrayList<Long>();
        Long userId;
        for (int i = 0; i < linesSize; i++) {
            userId = lines.get(i).getUserId();
            if (!users.containsKey(userId)) {
                userIds.add(userId);
                users.put(userId, UUID.randomUUID());
                this.utils.setup(users.get(userId));
            }
            totalTime += lines.get(i).timestamp;
        }
        this.ids = new HashMap<Long, Long>();
        this.filenames = new HashMap<Long, String>();
    }

    public void run() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        long oldTime = 0;
        itemsCount = 0;

        for (Action action : lines) {
            long testTime = action.getTimestamp();
            long sleep = testTime - oldTime - (end - start);
            oldTime = testTime;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            start = System.currentTimeMillis();
            performCommits(action);
            end = System.currentTimeMillis();

        }

        doChecks();
    }

    private void performCommits(Action action) {

        Long userId = action.getUserId();
        Long tempId = action.getTempId();

        logger.info("serverDummy2_doCommit_start,commitID=" + users.get(userId));
        try {
            doCommit(users.get(userId), action);
            itemsCount++;
        } catch (DAOException e1) {
            logger.error(e1);
        } catch (Exception ex) {
            logger.error(ex);
        }
        logger.info("serverDummy2_doCommit_end,commitID=" + users.get(userId));
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

    private void doChecks() {
        try {
            WorkspaceRMI workspace = this.handler.getWorkspace(users.get(311951037L));
            assert workspace != null;
            HashMap<Long, ItemRMI> items = workspace.getItems();
            assert items.size() == 1;
            ItemRMI item = items.get(ids.get(1427391262L));
            assert item.isFolder() == false;

        } catch (RemoteException ex) {
            Logger.getLogger(WorkloadFromFileBenchmark.class.getName()).log(Level.SEVERE, null, ex);
        }

        int workspaceItems = 0;
        for (UUID id : this.users.values()) {
            try {
                WorkspaceRMI workspace = this.handler.getWorkspace(id);
                //WorkspaceRMI workspace = workspaceDAO.getById(id);
                workspaceItems += workspace.getItems().size();
            } catch (RemoteException ex) {
                Logger.getLogger(WorkloadFromFileBenchmark.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (workspaceItems != itemsCount) {
            System.out.println("Something wrong happens...");
        }
        System.out.println(workspaceItems + " vs " + itemsCount);
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public InfinispanConnection getConnection() {
        return this.utils.getConnection();
    }
}
