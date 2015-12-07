package com.stacksync.syncservice.test.benchmark.normal;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.util.Config;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;
import static org.testng.Assert.assertEquals;


/**
 * @author Pierre Sutra
 */
@Test(testName = "testCommitInfinispan", groups = "unit", enabled = true)
public class TestCommitInfinispan extends MultipleCacheManagersTest {


   protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   protected static final int NCALLS = 1000;
   protected static final int MAX_ENTRIES = 100;
   protected static final int REPLICATION_FACTOR = 2;
   protected static final String PERSISTENT_STORAGE_DIR = "/tmp/aof-storage";

   private final static int NUMBER_TASKS = 1;
   private final static int NUMBER_COMMITS = 1000;
   private final static int NUMBER_USERS = 3;

   private static List<HotRodServer> servers = new ArrayList<>();
   private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();
   private static ConfigurationBuilder defaultBuilder;


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

   public BasicCacheContainer container(int i) {
      return remoteCacheManagers.get(i);
   }

   public Collection<BasicCacheContainer> containers() {
      return remoteCacheManagers;
   }

   public boolean addContainer() {
      int index = servers.size();

      // set-up data eviction and persistence
      if (MAX_ENTRIES!=Integer.MAX_VALUE) {
         defaultBuilder.eviction().maxEntries(MAX_ENTRIES);
         defaultBuilder.eviction().strategy(EvictionStrategy.LRU);

         File file = new File(PERSISTENT_STORAGE_DIR + "/" + index);
         if (file.exists()) {
            // clear previous stuff
            for (File children : file.listFiles()) {
               children.delete();
            }
            file.delete();
         }

         SingleFileStoreConfigurationBuilder storeConfigurationBuilder
               = defaultBuilder.persistence().addSingleFileStore();
         storeConfigurationBuilder.location(file.getPath());
         storeConfigurationBuilder.purgeOnStartup(true);
         storeConfigurationBuilder.fetchPersistentState(false);
         storeConfigurationBuilder.persistence().passivation(true);
      }

      // embedded cache manager
      addClusterEnabledCacheManager(defaultBuilder).getCache();
      waitForClusterToForm();

      // hotrod server
      HotRodServer server = HotRodTestingUtil.startHotRodServer(
            manager(index),
            11222+index);
      FilterConverterFactory factory = new FilterConverterFactory();
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, factory);
      server.startDefaultCache();
      servers.add(server);

      // remote manager
      RemoteCacheManager manager = new RemoteCacheManager(
            new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                  .addServers(server.getHost()+":"+server.getPort())
                  .marshaller((Marshaller) null)
                  .build());
      remoteCacheManagers.add(manager);

      System.out.println("Node " + manager+ " added.");
      return true;
   }

   public  boolean deleteContainer() {
      if (servers.size()==0) return false;
      int index = servers.size() - 1;

      // remote manager
      remoteCacheManagers.get(index).stop();
      remoteCacheManagers.remove(index);

      // hotrod server
      servers.get(index).stop();
      servers.remove(index);

      // embedded cache manager
      BasicCacheContainer manager = cacheManagers.get(index);
      cacheManagers.get(index).stop();
      cacheManagers.remove(index);

      waitForClusterToForm();
      System.out.println("Node " + manager + " deleted.");

      return true;
   }


   @Override
   protected void createCacheManagers() throws Throwable {
      createDefaultBuilder();

      for (int j = 0; j < getNumberOfManagers(); j++) {
         addContainer();
      }

      // Verify that default caches are started.
      for (int j = 0; j < getNumberOfManagers(); j++) {
         blockUntilCacheStatusAchieved(
               manager(j).getCache(), ComponentStatus.RUNNING, 10000);
      }

      waitForClusterToForm();

      assertEquals(manager(0).getTransport().getMembers().size(), getNumberOfManagers());

      AtomicObjectFactory.forCache(container(0).getCache());
   }

   // Helpers

   private void createDefaultBuilder() {
      defaultBuilder = getDefaultClusteredCacheConfig(CACHE_MODE,false);
      defaultBuilder
            .clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(getReplicationFactor())
            .locking().useLockStriping(false)
            .compatibility().enable();
      defaultBuilder.clustering().stateTransfer()
            .awaitInitialTransfer(true)
            .timeout(1000000)
            .fetchInMemoryState(true);
   }

}

