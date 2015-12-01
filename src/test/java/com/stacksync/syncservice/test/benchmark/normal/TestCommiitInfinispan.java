package com.stacksync.syncservice.test.benchmark.normal;

import org.infinispan.atomic.AtomicObjectFactoryRemoteTest;
import org.junit.Test;

/**
 * @author Pierre Sutra
 */
public class TestCommiitInfinispan extends AtomicObjectFactoryRemoteTest{

   private final static int NUMBER_TASKS = 3;
   private final static int NUMBER_COMMITS = 1000;

   public int getReplicationFactor() {
      return 2;
   }

   public int getNumberOfManagers() {
      return 3;
   }

   @Test
   public void commit() throws Exception{
      TestCommit testCommit = new TestCommit(NUMBER_TASKS,NUMBER_COMMITS);
      testCommit.commit();
   }

}

