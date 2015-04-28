/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class ServerDummy extends AServerDummy {

    private int linesSize;
    private long totalTime;

    protected HashMap<Long, UUID> users;
    protected ArrayList<Long> userIds;
    protected ArrayList<Action> lines;

    private int itemsCount;

    public ServerDummy(ConnectionPool pool, int numUsers, String fileName, int commitsPerMinute, int minutes) throws SQLException,
            NoStorageManagerAvailable,
            Exception {
        super(pool, commitsPerMinute, minutes);

        this.lines = new ArrayList<Action>();
        BufferedReader read = new BufferedReader(new FileReader(fileName));
        String line = read.readLine();
        String[] lineParts;
        while (line != null) {
            lineParts = line.split(",");
            if (lineParts[1].equals("new")) {
                lines.add(new New(lineParts));
            } else if (lineParts[1].equals("mod")) {
                lines.add(new Modify(lineParts));
            } else if (lineParts[1].equals("del")) {
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

        if (minutes != 0) {
            // Distance between commits in msecs
            long distance = (long) (1000 / (commitsPerMinute / 60.0));
            Random ran = new Random(System.currentTimeMillis());
            for (int i = 0; i < minutes; i++) {
                start = System.currentTimeMillis();
                for (int j = 0; j < commitsPerMinute; j++) {

                    int num = (new Random()).nextInt(userIds.size());
                    Long userId = userIds.get(num);
                    String str = "" + "0,new," + ran.nextLong() + ",,," + ran.nextInt() + "," + userId;
                    String[] lineParts = str.split(",");
                    Action action = new New(lineParts);

                    long startCommit = System.currentTimeMillis();
                    commit(action);
                    long endCommit = System.currentTimeMillis();

                    // If doCommit had no cost sleep would be distance but we have
                    // to take into account of the time that it takes
                    long sleep = distance - (endCommit - startCommit);
                    if (sleep > 0) {
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                end = System.currentTimeMillis();
                long minute = end - start;

                // I will forgive 5 seconds of delay...
                if (minute > 65 * 1000) {
                    // Notify error
                    logger.error("MORE THAN 65 SECONDS=" + (minute / 1000));
                }
            }
        } else {

            for (Action action : lines) {

                // If doCommit had no cost sleep would be distance but we have
                // to take into account of the time that it takes
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
        }

        int workspaceItems = 0;
        for (UUID id : this.users.values()) {
            try {
                WorkspaceRMI workspace = this.handler.getWorkspace(id);
                //WorkspaceRMI workspace = workspaceDAO.getById(id);
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

    public int getLinesSize() {
        return linesSize;
    }

    public int getItemsCount() {
        return itemsCount;
    }
}
