/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface DeviceDAO extends Remote {

    public DeviceRMI get(UUID id) throws RemoteException;

    public void add(DeviceRMI device) throws RemoteException;

    public void update(DeviceRMI device) throws RemoteException;

    public void deleteDevice(UUID id) throws RemoteException;

}
