package com.stacksync.syncservice.rmiserveri;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;

public interface UserDAORMIIfc extends Remote {

	public UserRMI findById(UUID id) throws RemoteException;
	
	public UserRMI getByEmail(String email) throws RemoteException;

	public List<UserRMI> findAll() throws RemoteException;
	
	public List<UserRMI> findByItemId(Long clientFileId) throws RemoteException;

	public void add(UserRMI user) throws RemoteException;

	public void update(UserRMI user) throws RemoteException;

	public void delete(UUID id) throws RemoteException;
	
}
