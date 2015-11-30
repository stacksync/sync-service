package com.stacksync.syncservice.test.benchmark.normal;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class CommitTask implements Runnable {

   private Handler handler;
   private int numberOfCommits;

   public CommitTask(Handler handler, int numberOfCommits) {
      this.handler = handler;
      this.numberOfCommits = numberOfCommits;
   }

   @Override
   public void run() {
      try {
         List<WorkspaceRMI> workspaces = new ArrayList<>();
         long startTotal = System.currentTimeMillis();
         for (int i = 0; i < numberOfCommits; i++) {
            try {
               String metadata = CommonFunctions.generateObjects(1, UUID.randomUUID());
               JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
               List<ItemMetadataRMI> objects = TestCommit.getObjectMetadata(rawObjects);
               UserRMI user = new UserRMI(UUID.randomUUID());
               DeviceRMI device = new DeviceRMI(UUID.randomUUID(), "android", user);
               WorkspaceRMI workspace = new WorkspaceRMI(UUID.randomUUID(), 1, user, false, false);
               handler.doCommit(user, workspace, device, objects);
               workspaces.add(workspace);
            } catch (DAOException e) {
               e.printStackTrace();
            }
         }

         long totalTime = System.currentTimeMillis() - startTotal;
         System.out.println("Total time --> " + totalTime
               + " ms ["+( 1000 * ((float)numberOfCommits) / (double)totalTime) +" ops/sec]");

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
