/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.sql.SQLException;
import java.util.UUID;

import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class ReadFile extends AReadFile {

    private int linesSize;
    private long totalTime;

    protected HashMap<Long, UUID> users;
    protected ArrayList<Long> userIds;
    protected ArrayList<Action> lines;

    private int itemsCount;

    public ReadFile(ConnectionPool pool, int numUsers, String fileName) throws SQLException,
            NoStorageManagerAvailable,
            Exception {
        super(pool);

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
                this.setup(users.get(userId));
            }
            totalTime += lines.get(i).timestamp;
        }
    }

    @Override
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
            commit(action);
            end = System.currentTimeMillis();

        }
        
        try {
            WorkspaceRMI workspace = this.handler.getWorkspace(users.get(311951037L));
            assert workspace != null;
            HashMap<Long, ItemRMI> items = workspace.getItems();
            assert items.size() == 1;
            ItemRMI item = items.get(ids.get(1427391262L));
            assert item.isFolder() == false;
            
            
        } catch (RemoteException ex) {
            Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
        }

        int workspaceItems = 0;
        for (UUID id : this.users.values()) {
            try {
                WorkspaceRMI workspace = this.handler.getWorkspace(id);
                //WorkspaceRMI workspace = workspaceDAO.getById(id);
                workspaceItems += workspace.getItems().size();
            } catch (RemoteException ex) {
                Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (workspaceItems != itemsCount) {
            System.out.println("Something wrong happens...");
        }
        System.out.println(workspaceItems + " vs " + itemsCount);
    }

    private void commit(Action action) {

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

    public int getItemsCount() {
        return itemsCount;
    }
}
