package com.stacksync.syncservice.test.benchmark.normal;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;
import org.infinispan.atomic.AtomicObjectFactoryRemoteTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Test(testName = "testCommitInfinispan", groups = "unit", enabled = true)
public class TestCommiitInfinispan extends AtomicObjectFactoryRemoteTest{

   private final static int NUMBER_TASKS = 2;
   private final static int NUMBER_COMMITS = 1000;
   private final static int NUMBER_USERS = 3;

   public int getReplicationFactor() {
      return 2;
   }

   public int getNumberOfManagers() {
      return 3;
   }

   @Test
   public void commit() throws Exception{

      Config.loadProperties();
      Properties properties = Config.getProperties();
      properties.setProperty("infinispan_host", "127.0.0.1:11222");
      String datasource = Config.getDatasource();
      ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
      pool.getConnection().cleanup();

      TestCommit testCommit = new TestCommit(NUMBER_TASKS,NUMBER_COMMITS, NUMBER_USERS, false);
      testCommit.populate(pool);
      List<UUID> uuids = testCommit.execute(pool);
      Handler handler = new SQLSyncHandler(pool);
      int totalItems = 0;
      for (UUID uuid: uuids) {
         WorkspaceRMI workspace = handler.getWorkspace(uuid);
         totalItems +=workspace.getItems().size();
      }
      assert totalItems==NUMBER_TASKS*NUMBER_COMMITS: totalItems +" "+ NUMBER_TASKS*NUMBER_COMMITS;

   }

}

