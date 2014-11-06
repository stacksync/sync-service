package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.ResultSet;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class DeviceDAORMISer extends UnicastRemoteObject implements
		DeviceDAORMIIfc {

	public DeviceDAORMISer() throws RemoteException {
		super();
	}

	@Override
	public DeviceRMI get(UUID deviceID) throws RemoteException {
		DeviceRMI device = null;

		return device;
	}

	@Override
	public void add(DeviceRMI device) throws RemoteException {

	}

	@Override
	public void update(DeviceRMI device) throws RemoteException {

	}

	@Override
	public void delete(UUID deviceID) throws RemoteException {

	}

	private DeviceRMI mapDevice(ResultSet resultSet) {

		DeviceRMI device = new DeviceRMI();

		return device;
	}

}
