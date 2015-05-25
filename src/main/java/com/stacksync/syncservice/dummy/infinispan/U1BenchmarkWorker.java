/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.dummy.infinispan.actions.Action;
import com.stacksync.syncservice.dummy.infinispan.actions.ActionFactory;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;

public class U1BenchmarkWorker extends Thread {

    private final Logger logger = Logger.getLogger(U1Workload.class.getName());
    private File file;
    private Handler handler;

    public U1BenchmarkWorker(ConnectionPool pool, File file) throws Exception {
        handler = new SQLSyncHandler(pool);
        this.file = file;
    }

    @Override
    public void run() {

        try {
            String line;
            BufferedReader buff = new BufferedReader(new FileReader(file));

            long execTime = 0L;

            long start = System.currentTimeMillis();
            line = buff.readLine();

            if (line == null) {
                buff.close();
                return;
            }

            do {
                try {
                    String[] words = line.split(",");
                    double timestamp = Double.parseDouble(words[0]);
                    long t = (long) (timestamp * 1000);
                    String op = words[1];
                    Long fileId = Long.parseLong(words[2]);
                    String fileType = words[3];
                    String fileMime = words[4];
                    Long fileSize = tryParseLong(words[5]);
                    Long fileVersion = tryParseLong(words[6]);
                    UUID userId = UUID.fromString(words[8]);

                    long sleep = t - execTime;
                    if (sleep > 0) {
                        Thread.sleep(sleep);
                    }

                    Action action = ActionFactory.getNewAction(op, handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
                    action.doCommit();

                    long end = System.currentTimeMillis();

                    execTime = end - start;
                } catch (Exception e) {
                    logger.error(e);
                }

            } while ((line = buff.readLine()) != null);

            buff.close();

        } catch (Exception e) {
            logger.error(e);
        }
    }

    private long tryParseLong(String num) {
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}