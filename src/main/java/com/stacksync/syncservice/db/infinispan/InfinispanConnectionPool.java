/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import java.sql.SQLException;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanConnectionPool extends ConnectionPool {

    private RemoteCacheManager cacheManager;
    private InfinispanConnection connection;

    public InfinispanConnectionPool() throws DAOConfigurationException {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.maxRetries(1);
        builder.addServers("192.168.2.2");
        cacheManager = new RemoteCacheManager(builder.build());
        connection = new InfinispanConnection(cacheManager.getCache());
    }

    /*private void createDistributedCacheManager() {

        if (cacheManager != null && cacheManager.getStatus() == ComponentStatus.RUNNING) {
            System.out.println("CacheManager already started, nothing to do here");
            return;
        }

        String infinispanConfig = System.getProperties().getProperty("infinispanConfigFile");
        if (infinispanConfig != null) {
            try {
                cacheManager = new DefaultCacheManager(infinispanConfig);
            } catch (IOException e) {
                System.out.println("File " + infinispanConfig + " is corrupted.");
            }
        }

        if (cacheManager == null) {
            for (int i = 0; i < NumCaches; i++) {
                GlobalConfiguration globalConfig = new GlobalConfigurationBuilder()
                        .transport().defaultTransport()
                        .build();
                ConfigurationBuilder cb = new ConfigurationBuilder();
                Configuration c = cb.
                        transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).
                        clustering().cacheMode(CacheMode.DIST_SYNC).
                        build();
                cacheManager = new DefaultCacheManager(globalConfig, c);
                /*GlobalConfiguration globalConfig = new GlobalConfigurationBuilder()
                 .transport().defaultTransport()
                 .build();
                 ConfigurationBuilder cb = new ConfigurationBuilder();
                 Configuration c = cb.clustering().cacheMode(CacheMode.DIST_SYNC).
                 hash().numOwners(1).build();
                 this.cacheManager = new DefaultCacheManager(globalConfig, c);*
            }
        }
        cacheManager.start();

    }

    private void createRemoteCacheManager() {

        if (remoteCacheManager != null) { //&& cacheManager.getStatus() == ComponentStatus.RUNNING) {
            System.out.println("CacheManager already started, nothing to do here");
            return;
        }

        org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        cb.tcpNoDelay(true)
                .connectionPool()
                .numTestsPerEvictionRun(3)
                .testOnBorrow(false)
                .testOnReturn(false)
                .testWhileIdle(true)
                .addServer()
                .host("10.30.102.242")
                .port(11222);

        remoteCacheManager = new RemoteCacheManager(cb.build());
        remoteCacheManager.start();
        rCache = remoteCacheManager.getCache();
    }*/

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return connection;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
