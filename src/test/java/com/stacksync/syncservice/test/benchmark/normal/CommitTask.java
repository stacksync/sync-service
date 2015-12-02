package com.stacksync.syncservice.test.benchmark.normal;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * @author Pierre Sutra
 */
public class CommitTask implements Callable<Float> {

   private Handler handler;
   private int numberOfCommits;
   private final List<WorkspaceRMI> workspaces;

   public CommitTask(Handler handler, int numberOfCommits, final List<WorkspaceRMI> workspaces) {
      this.handler = handler;
      this.numberOfCommits = numberOfCommits;
      this.workspaces = workspaces;
   }

   @Override
   public Float call() {
      float throughput = 0;
      try {
         Random random = new Random(System.currentTimeMillis());
         long startTotal = System.currentTimeMillis();
         for (int i = 0; i < numberOfCommits; i++) {
            try {
               String metadata = CommonFunctions.generateObjects(1, UUID.randomUUID());
               JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
               List<ItemMetadataRMI> objects = TestCommit.getObjectMetadata(rawObjects);
               WorkspaceRMI workspace = workspaces.get(random.nextInt(workspaces.size()));
               UserRMI user = workspace.getOwner();
               DeviceRMI device = new DeviceRMI(UUID.randomUUID(), "android", user);
               handler.doCommit(user, workspace, device, objects);
               workspaces.add(workspace);
            } catch (DAOException e) {
               e.printStackTrace();
            }
         }

         long totalTime = System.currentTimeMillis() - startTotal;
         throughput = ( 1000 * ((float)numberOfCommits) / (float)totalTime);
         System.out.println("time -> " + totalTime + " ms ["+throughput +" ops/sec]");

      } catch (Exception e) {
         e.printStackTrace();
      }

      return throughput;
   }

}
