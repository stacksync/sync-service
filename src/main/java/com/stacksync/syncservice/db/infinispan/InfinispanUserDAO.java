/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface InfinispanUserDAO extends Remote {

    public UserRMI findById(UUID id) throws RemoteException;

    public UserRMI getByEmail(String email) throws RemoteException;

    public List<UserRMI> findAll() throws RemoteException;

    public List<UserRMI> findByItemId(Long clientFileId) throws RemoteException;

    public void add(UserRMI user) throws RemoteException;

    public void update(UserRMI user) throws RemoteException;

    public void deleteUser(UUID id) throws RemoteException;

}