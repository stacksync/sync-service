/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanConnection;
import com.stacksync.syncservice.db.infinispan.InfinispanDeviceDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanUserDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanWorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class StaticBenchmark extends Thread {

    protected final Logger logger = Logger.getLogger(StaticBenchmark.class.getName());

    private int commitsPerSecond, minutes;
    private Handler handler;
    private ArrayList<UUID> userIds;
    private int itemsCount;
    private AOFUtil utils;

    public StaticBenchmark(ConnectionPool pool, int numUsers, int commitsPerSecond, int minutes) throws SQLException, NoStorageManagerAvailable, Exception {

        this.utils = new AOFUtil(pool);
        this.handler = new SQLSyncHandler(pool);
        this.commitsPerSecond = commitsPerSecond;
        this.minutes = minutes;
        userIds = new ArrayList<UUID>();

        for (int i = 0; i < numUsers; i++) {
            UUID userId = UUID.randomUUID();
            userIds.add(userId);
            this.utils.setup(userId);
        }
    }

    @Override
    public void run() {
        itemsCount = 0;
        
        // Distance between commits in msecs
        long distance = (long) (1000 / commitsPerSecond);
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < minutes; i++) {
            long startMinute = System.currentTimeMillis();
            performCommits(distance, random);
            long endMinute = System.currentTimeMillis();
            long minute = endMinute - startMinute;

            // I will forgive 5 seconds of delay...
            if (minute > 65 * 1000) {
                // Notify error
                logger.error("MORE THAN 65 SECONDS=" + (minute / 1000));
            }
        }

        doChecks();
    }

    public void performCommits(long distance, Random random) {

        for (int j = 0; j < commitsPerSecond * 60; j++) {
            String id = UUID.randomUUID().toString();

            logger.info("serverDummy2_doCommit_start,commitID=" + id);
            long start = System.currentTimeMillis();
            try {
                UUID userID = this.userIds.get(random.nextInt(userIds.size()));
                doCommit(userID, id);
                itemsCount++;
            } catch (Exception ex) {
                logger.error(ex);
            }

            long end = System.currentTimeMillis();
            logger.info("serverDummy2_doCommit_end,commitID=" + id);

            // If doCommit had no cost sleep would be distance but we have
            // to take into account of the time that it takes
            long sleep = distance - (end - start);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void doCommit(UUID uuid, String id) throws Exception {
        // Create user info
        UserRMI user = new UserRMI(uuid);
        DeviceRMI device = new DeviceRMI(uuid);
        WorkspaceRMI workspace = new WorkspaceRMI(uuid);

        // Create a ItemMetadata List
        List<ItemMetadata> items = new ArrayList<ItemMetadata>();
        items.add(TestUtil.createItemMetadata(uuid));

        logger.info("hander_doCommit_start,commitID=" + id);
        handler.doCommit(user, workspace, device, items);
        logger.info("hander_doCommit_end,commitID=" + id);
    }

    private void doChecks() {
        int workspaceItems = 0;
        for (UUID id : this.userIds) {
            try {
                WorkspaceRMI workspace = this.handler.getWorkspace(id);
                //WorkspaceRMI workspace = workspaceDAO.getById(id);
                workspaceItems += workspace.getItems().size();
            } catch (RemoteException ex) {
                logger.error(ex);
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
