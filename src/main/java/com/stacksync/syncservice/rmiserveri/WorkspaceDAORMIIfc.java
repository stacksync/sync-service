package com.stacksync.syncservice.rmiserveri;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;

public interface WorkspaceDAORMIIfc extends Remote {

	public WorkspaceRMI getById(UUID id) throws RemoteException;

	public List<WorkspaceRMI> getByUserId(UUID userId) throws RemoteException;
	
	public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId) throws RemoteException;
	
	public WorkspaceRMI getByItemId(Long itemId) throws RemoteException;

	public void add(WorkspaceRMI workspace) throws RemoteException;

	public void update(UserRMI user, WorkspaceRMI workspace) throws RemoteException;

	public void addUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException;
	
	public void deleteUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException;

	public void delete(UUID id) throws RemoteException;
	
	public List<UserWorkspaceRMI> getMembersById(UUID workspaceId) throws RemoteException;

}
