package com.stacksync.syncservice.test.benchmark.normal;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.test.benchmark.Constants;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * @author Pierre Sutra
 */
public class CommitTask implements Callable<Float> {

   private final Handler handler;
   private final int numberCommits;
   private final List<UUID> users;
   private boolean verbose;

   public CommitTask(Handler handler, int numberCommits, List<UUID> users, boolean verbose) {
      this.handler = handler;
      this.numberCommits = numberCommits;
      this.verbose = verbose;
      this.users = users;
   }

   @Override
   public Float call() {
      float throughput = 0;
      try {
         long startTotal = System.currentTimeMillis();
         SecureRandom random = new SecureRandom(UUID.randomUUID().toString().getBytes());
         for (int i = 0; i < numberCommits; i++) {
            try {
               long start = System.currentTimeMillis();

               int next = Math.abs(random.nextInt());
               UUID userId = users.get(next % users.size());

               // generate metadata
               String metadata = CommonFunctions.generateObjects(1, UUID.randomUUID());
               JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
               List<ItemMetadataRMI> objects = TestCommit.getObjectMetadata(rawObjects, Constants.DEVICE_ID);

               handler.doCommit(userId, userId, Constants.DEVICE_ID, objects);

               if (verbose) System.out.println(System.currentTimeMillis()-start);
            } catch (Exception | DAOException e) {
               e.printStackTrace();
            }
         }

         long totalTime = System.currentTimeMillis() - startTotal;
         throughput = ( 1000 * ((float) numberCommits) / (float)totalTime);
         System.out.println("time -> " + totalTime + " ms ["+throughput +" ops/sec]");

      } catch (Exception e) {
         e.printStackTrace();
      }

      return throughput;
   }

}
