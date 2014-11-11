package com.stacksync.syncservice.rmiserveri;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;

public interface DeviceDAORMIIfc extends Remote {

	public DeviceRMI get(UUID id) throws RemoteException;

	public void add(DeviceRMI device) throws RemoteException;

	public void update(DeviceRMI device) throws RemoteException;

	public void delete(UUID id) throws RemoteException;

}
