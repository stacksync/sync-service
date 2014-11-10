package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.util.Constants;

public class DeviceDAORMISer extends UnicastRemoteObject implements
		DeviceDAORMIIfc {
	
	List<DeviceRMI> llistat;

	public DeviceDAORMISer() throws RemoteException {
		llistat = new ArrayList<DeviceRMI>();
	}

	@Override
	public DeviceRMI get(UUID deviceID) throws RemoteException {
		
		// API device ID is not stored in the database
		if(deviceID == Constants.API_DEVICE_ID){
			return new DeviceRMI(Constants.API_DEVICE_ID);
		}
		
		DeviceRMI device = null;
		
		for (DeviceRMI d : llistat){
			if (d.getId() == deviceID){
				device = d;
			}
		}

		return device;
	}

	@Override
	public void add(DeviceRMI device) throws RemoteException {
		if (!device.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}
		
		boolean exist = false;
		
		for (DeviceRMI d: llistat){
			if (d.getId() == device.getId()){
				exist = true;
			}
		}
		
		if (!exist) {
			llistat.add(device);
			System.out.println("ADDED");
		} else
			System.out.println("EXISTING DEVICE ID");
	}

	@Override
	public void update(DeviceRMI device) throws RemoteException {
		if (device.getId() == null || !device.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}
		
		boolean exist = false;
		DeviceRMI d1 = null;
		
		for (DeviceRMI d: llistat){
			if (d.getId() == device.getId()){
				exist = true;
				d1 = d;
			}
		}
		
		if (exist) {
			llistat.remove(d1);
			llistat.add(device);
			System.out.println("UPDATED");
		} else
			System.out.println("DEVICE ID DOESN'T EXIST");
	}

	@Override
	public void delete(UUID deviceID) throws RemoteException {
		boolean exist = false;
		DeviceRMI d1 = null;
		
		for (DeviceRMI d: llistat){
			if (d.getId() == deviceID){
				exist = true;
				d1 = d;
			}
		}
		
		if (exist) {
			llistat.remove(d1);
			System.out.println("DELETED");
		} else
			System.out.println("DEVICE ID DOESN'T EXIST");
	}

}
