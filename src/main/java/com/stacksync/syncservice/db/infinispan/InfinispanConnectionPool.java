/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import java.sql.Connection;
import java.sql.SQLException;
import org.postgresql.ds.PGPoolingDataSource;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanConnectionPool extends ConnectionPool {

    private PGPoolingDataSource source;

    public InfinispanConnectionPool() throws DAOConfigurationException {
        try {
            Class.forName("org.postgresql.Driver");

            // Initialize a pooling DataSource
            source = new PGPoolingDataSource();
            source.getConnection().close();

        } catch (ClassNotFoundException e) {
            throw new DAOConfigurationException("Infinispan JDBC driver not found", e);
        } catch (SQLException e) {
            throw new DAOConfigurationException("SQLException catched at DAOFactory", e);
        }

    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return source.getConnection();
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

}
