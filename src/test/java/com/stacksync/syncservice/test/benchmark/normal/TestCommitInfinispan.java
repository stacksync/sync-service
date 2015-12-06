package com.stacksync.syncservice.test.benchmark.normal;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;
import org.infinispan.atomic.AtomicObjectFactoryRemoteTest;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * @author Pierre Sutra
 */
@Test(testName = "testCommitInfinispan", groups = "unit", enabled = true)
public class TestCommitInfinispan extends AtomicObjectFactoryRemoteTest{

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
      testCommit.createUsers();
      testCommit.populate(pool);
      pool.getConnection().close();

      pool = ConnectionPoolFactory.getConnectionPool(datasource);
      testCommit = new TestCommit(NUMBER_TASKS,NUMBER_COMMITS, NUMBER_USERS, false);
      testCommit.createUsers();
      testCommit.execute(pool,false);
      pool.getConnection().close();

   }

}

