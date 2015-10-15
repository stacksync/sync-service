/**
 *
 */
package com.stacksync.syncservice.dummy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.dummy.actions.NewItem;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;
import org.apache.log4j.Logger;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class FillDBWithFiles2 extends Thread {

    private final Logger logger = Logger.getLogger(FillDBWithFiles2.class.getName());
    private BlockingQueue<String> queue;
    private Handler handler;
    private boolean killed = false;

    public FillDBWithFiles2(ConnectionPool pool, BlockingQueue<String> queue) throws SQLException, NoStorageManagerAvailable {
        this.queue = queue;
        handler = new SQLSyncHandler(pool);
    }

    @Override
    public void run() {
        String line;
        int i = 0, commits = 0;
        logger.info("start run!");
        while (!killed) {
            try {
                line = queue.take();
                String[] words = line.split(",");
                Long itemId = Long.parseLong(words[0]);
                UUID userId = UUID.fromString(words[1]);
                commits++;
                doCommit(userId, itemId);
                i++;
                logger.info( commits + " - " + i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (DAOException e) {
                e.printStackTrace();
            }
        }
    }

    public void kill() {
        killed = true;
        interrupt();
    }

    public void doCommit(UUID userId, long fileId) throws DAOException {
        NewItem newItem = new NewItem(handler, userId, fileId, null, null, null, 1L);
        newItem.doCommit();
    }

    public static void main(String[] args) throws Exception {
        // args = new String[] { "day_files_without_new.csv" };

        if (args.length != 2) {
            System.err.println("Usage: file_path num_threads");
            System.exit(0);
        }

        String configPath = "config.properties";
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

        BlockingQueue<String> queue = new ArrayBlockingQueue<String>(700000);

        int numThread = Integer.parseInt(args[1]);

        String line;
        BufferedReader buff = new BufferedReader(new FileReader(new File(args[0])));
        int i = 0;
        while ((line = buff.readLine()) != null) {
            try {
                queue.add(line);
                if (i % 1000 == 0) {
                    System.out.println(i);
                }
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        FillDBWithFiles2[] threads = new FillDBWithFiles2[numThread];
        for (i = 0; i < numThread; i++) {
            threads[i] = new FillDBWithFiles2(pool, queue);
            threads[i].start();
        }

        buff.close();

        System.out.println("Waiting until queue is empty.");
        while (!queue.isEmpty()) {
            System.out.println("queue size: " + queue.size());
            Thread.sleep(1000);
        }

        System.out.println("Queue is empty - let's kill threads");
        for (FillDBWithFiles2 t : threads) {
            t.kill();
        }
    }
}
