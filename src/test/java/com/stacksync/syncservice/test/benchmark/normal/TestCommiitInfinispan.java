package com.stacksync.syncservice.test.benchmark.normal;

import org.infinispan.atomic.AtomicObjectFactoryRemoteTest;
import org.junit.Test;

/**
 * @author Pierre Sutra
 */
public class TestCommiitInfinispan extends AtomicObjectFactoryRemoteTest{

   private final static int NUMBER_TASKS = 3;
   private final static int NUMBER_COMMITS = 10000;
   private final static int NUMBER_WORKSPACES = 1000;
   private final static int NUMBER_USERS = 1000;

   public int getReplicationFactor() {
      return 1;
   }

   public int getNumberOfManagers() {
      return 1;
   }

   @Test
   public void commit() throws Exception{
      TestCommit testCommit = new TestCommit(NUMBER_TASKS,NUMBER_COMMITS, NUMBER_WORKSPACES, NUMBER_USERS);
      testCommit.commit();
   }

}

