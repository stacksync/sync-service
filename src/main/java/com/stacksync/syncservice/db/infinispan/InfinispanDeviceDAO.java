/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface InfinispanDeviceDAO extends Remote {

    public DeviceRMI get(UUID id) throws RemoteException;

    public void add(DeviceRMI device) throws RemoteException;

    public void update(DeviceRMI device) throws RemoteException;

    public void deleteDevice(UUID id) throws RemoteException;

}
