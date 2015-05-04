/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanConnectionPool extends ConnectionPool {

    private RemoteCacheManager remoteCacheManager;
    private EmbeddedCacheManager cacheManager;
    private final Integer NumCaches = 1;
    
    public InfinispanConnectionPool() throws DAOConfigurationException {
        /*createRemoteCacheManager();*/
        createDistributedCacheManager();
        System.out.println("Cache manager started.");
    }
    
    private void createDistributedCacheManager() {

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
                Configuration c = cb.clustering().cacheMode(CacheMode.DIST_SYNC).
                        hash().numOwners(3).build();
                this.cacheManager = new DefaultCacheManager(globalConfig, c);
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
                .host("localhost")
                .port(11222);
        
        remoteCacheManager = new RemoteCacheManager(cb.build());
        remoteCacheManager.start();
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return new InfinispanConnection(cacheManager.getCache());
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

}
