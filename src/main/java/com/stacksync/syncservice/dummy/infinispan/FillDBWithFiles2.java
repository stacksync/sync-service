/**
 *
 */
package com.stacksync.syncservice.dummy.infinispan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.dummy.infinispan.actions.NewItem;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class FillDBWithFiles2 extends Thread {

    private BlockingQueue<String> queue;
    private Handler handler;
    private boolean killed = false;

    public FillDBWithFiles2(ConnectionPool pool, BlockingQueue<String> queue) throws Exception {
        this.queue = queue;
        handler = new SQLSyncHandler(pool);
    }

    @Override
    public void run() {
        String line;
        while (!killed) {
            int counter=0;
            try {
                line = queue.take();
                String[] words = line.split(",");
                Long itemId = Long.parseLong(words[0]);
                UUID userId = UUID.fromString(words[1]);

                doCommit(userId, itemId);
                counter++;
                if (counter%100 == 0){
                    System.out.println("Still adding: "+counter);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void kill() {
        killed = true;
        interrupt();
    }

    public void doCommit(UUID userId, long fileId) throws Exception {
        NewItem newItem = new NewItem(handler, userId, fileId, null, null, null, 1L);
        newItem.doCommit();
    }

    public static void main(String[] args) throws Exception {

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

        FillDBWithFiles2[] threads = new FillDBWithFiles2[numThread];

        for (int i = 0; i < numThread; i++) {
            threads[i] = new FillDBWithFiles2(pool, queue);
            threads[i].start();
        }

        String line;
        BufferedReader buff = new BufferedReader(new FileReader(new File(args[0])));
        int i = 0;
        while ((line = buff.readLine()) != null) {
            try {
                queue.add(line);
                if (i % 100 == 0) {
                    System.out.println(i);
                }
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        buff.close();

        while (!queue.isEmpty()) {
            Thread.sleep(1000);
        }

        for (FillDBWithFiles2 t : threads) {
            t.kill();
        }
    }
}
