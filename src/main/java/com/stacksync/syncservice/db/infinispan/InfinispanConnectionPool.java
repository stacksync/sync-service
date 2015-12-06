/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.util.Config;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanConnectionPool extends ConnectionPool {

   private RemoteCacheManager cacheManager;
   private InfinispanConnection connection;

   public InfinispanConnectionPool() throws DAOConfigurationException {
      Properties properties = Config.getProperties();
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.maxRetries(1);
      builder.addServers((String) properties.get("infinispan_host"));
      cacheManager = new RemoteCacheManager(builder.build());
      connection = new InfinispanConnection(cacheManager.getCache());
   }

   @Override
   public Connection getConnection() throws SQLException {
      try {
         return connection;
      } catch (Exception e) {
         throw new SQLException(e);
      }
   }
}
