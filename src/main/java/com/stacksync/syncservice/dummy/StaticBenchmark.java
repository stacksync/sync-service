/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import omq.common.broker.Broker;
import omq.exception.AlreadyBoundException;
import omq.exception.RemoteException;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.requests.CommitRequest;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.util.Config;
import com.stacksync.syncservice.util.Constants;

/**
 *
 * @author sergi
 */
public class StaticBenchmark extends Thread {

    protected final Logger logger = Logger.getLogger(StaticBenchmark.class
            .getName());

    protected static final int CHUNK_SIZE = 512 * 1024;

    private Broker broker;
    private int commitsPerSecond, minutes;
    private ISyncService[] shardProxies;
    private WorkspaceImpl[] workspaces;

    public StaticBenchmark(Broker broker, int numUsers, int commitsPerSecond,
            int minutes) throws SQLException, NoStorageManagerAvailable,
            AlreadyBoundException {
        this.broker = broker;
        this.commitsPerSecond = commitsPerSecond;
        this.minutes = minutes;

        shardProxies = new ISyncService[numUsers];
        workspaces = new WorkspaceImpl[numUsers];

        for (int i = 0; i < numUsers; i++) {
            try {
                Properties env = broker.getEnvironment();
                String syncServerExchange = env.getProperty(
                        Constants.PROP_OMQ_EXCHANGE,
                        Constants.DEFAULT_OMQ_EXCHANGE);

                UUID clientId = UUID.randomUUID();
                shardProxies[i] = broker.lookup(clientId.toString(),
                        ISyncService.class);
                shardProxies[i].createUser(clientId);

                env.setProperty(Constants.PROP_OMQ_EXCHANGE,
                        "rpc_return_exchange");

                workspaces[i] = new WorkspaceImpl();
                broker.bind(clientId.toString(), workspaces[i]);

                env.setProperty(Constants.PROP_OMQ_EXCHANGE, syncServerExchange);
            } catch (RemoteException ex) {
                logger.error(ex);
            }
        }
    }

    @Override
    public void run() {
        Random ran = new Random(System.currentTimeMillis());

        // Distance between commits in msecs
        long distance = (long) (1000 / commitsPerSecond);

        // Every iteration takes a minute
        for (int i = 0; i < minutes; i++) {

            long startMinute = System.currentTimeMillis();
            for (int j = 0; j < commitsPerSecond * 60; j++) {
                String id = UUID.randomUUID().toString();

                long start = System.currentTimeMillis();
                doCommit(shardProxies[ran.nextInt(shardProxies.length)], ran,
                        1, 8);
                long end = System.currentTimeMillis();

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

    }

    public void doCommit(ISyncService syncServer, Random ran, int min, int max) {
        UUID id = UUID.fromString(syncServer.getRef());

        // Create a ItemMetadata List
        List<ItemMetadata> items = new ArrayList<ItemMetadata>();
        items.add(createItemMetadata(ran, min, max, id));

        // Create a CommitRequest
        CommitRequest commitRequest = new CommitRequest(id, id, id, items);

        logger.info("RequestID=" + commitRequest.getRequestId());
        syncServer.commit(commitRequest);

    }

    private ItemMetadata createItemMetadata(Random ran, int min, int max,
            UUID deviceId) {
        String[] mimes = {"pdf", "php", "java", "docx", "html", "png", "jpeg",
            "xml"};

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

        ItemMetadata itemMetadata = new ItemMetadata(id, version, deviceId,
                parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);
        itemMetadata.setChunks(chunks);
        itemMetadata.setTempId((long) ran.nextInt(10));

        return itemMetadata;
    }

    private String doHash(String str) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {

            System.err
                    .println("Usage: commitsPerSecond numUsers minutes threads");
            System.exit(0);
        }

        int commitsPerSecond = Integer.parseInt(args[0]);
        int numUsers = Integer.parseInt(args[1]);
        int minutes = Integer.parseInt(args[2]);
        int numThreads = Integer.parseInt(args[3]);

        Config.loadProperties("config.properties");
        Broker broker = new Broker(Config.getProperties());

        StaticBenchmark[] threads = new StaticBenchmark[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new StaticBenchmark(broker, numUsers,
                    commitsPerSecond, minutes);
            threads[i].start();
        }

        for (StaticBenchmark t : threads) {
            t.join();
        }

        broker.stopBroker();

    }
}
