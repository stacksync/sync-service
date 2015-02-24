/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.postgresql;

import com.stacksync.syncservice.db.Connection;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class PostgresqlConnection implements Connection {

    private java.sql.Connection connection;

    public PostgresqlConnection(java.sql.Connection connection) {
        this.connection = connection;
    }
    
    @Override
    public void close() throws Exception {
        this.connection.close();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws Exception {
        this.connection.setAutoCommit(autoCommit);
    }

    @Override
    public void commit() throws Exception {
        this.connection.commit();
    }

    @Override
    public void rollback() throws Exception {
        this.connection.rollback();
    }
    
    public java.sql.Connection getConnection() {
        return this.connection;
    }
    
}
