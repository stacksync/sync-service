/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.infinispan.InfinispanWorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class ServerDummy extends AServerDummy {

    private UUID[] uuids;

    public ServerDummy(ConnectionPool pool, int numUsers, int commitsPerMinute, int minutes) throws SQLException,
            NoStorageManagerAvailable,
            Exception {
        super(pool, commitsPerMinute, minutes);

        uuids = new UUID[numUsers];
        for (int i = 0; i < numUsers; i++) {
            uuids[i] = UUID.randomUUID();
            this.setup(uuids[i]);
        }
    }

    @Override
    public void run() {
        Random ran = new Random(System.currentTimeMillis());
        // Distance between commits in msecs
        long distance = (long) (1000 / (commitsPerMinute / 60.0));

        int itemsCount = 0;
        // Every iteration takes a minute
        for (int i = 0; i < minutes; i++) {

            long startMinute = System.currentTimeMillis();
            for (int j = 0; j < commitsPerMinute; j++) {
                String id = UUID.randomUUID().toString();

                logger.info("serverDummy2_doCommit_start,commitID=" + id);
                long start = System.currentTimeMillis();
                try {
                    doCommit(uuids[ran.nextInt(uuids.length)], ran, 1, 8, id);
                    itemsCount++;
                } catch (DAOException e1) {
                    logger.error(e1);
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
            long endMinute = System.currentTimeMillis();
            long minute = endMinute - startMinute;

            // I will forgive 5 seconds of delay...
            if (minute > 65 * 1000) {
                // Notify error
                logger.error("MORE THAN 65 SECONDS=" + (minute / 1000));
            }
        }

        int workspaceItems = 0;
        for (UUID id : this.uuids) {
            try {
                WorkspaceRMI workspace = this.handler.getWorkspace(id);
                //WorkspaceRMI workspace = workspaceDAO.getById(id);
                List<ItemRMI> items = workspace.getItems();
                workspaceItems += workspace.getItems().size();
            } catch (RemoteException ex) {
                Logger.getLogger(ServerDummy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (workspaceItems != itemsCount) {
            System.out.println("Something wrong happens...");
        }
        System.out.println(workspaceItems + " vs " + itemsCount);
    }
}
