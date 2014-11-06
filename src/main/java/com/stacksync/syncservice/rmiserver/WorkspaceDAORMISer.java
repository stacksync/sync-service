package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class WorkspaceDAORMISer extends UnicastRemoteObject implements
		WorkspaceDAORMIIfc {

	public WorkspaceDAORMISer() throws RemoteException {
		super();
	}

	@Override
	public WorkspaceRMI getById(UUID workspaceID) throws RemoteException {
		WorkspaceRMI workspace = null;

		return workspace;
	}

	@Override
	public List<WorkspaceRMI> getByUserId(UUID userId) throws RemoteException {

		List<WorkspaceRMI> workspaces = new ArrayList<WorkspaceRMI>();

		return workspaces;
	}

	@Override
	public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId)
			throws RemoteException {

		WorkspaceRMI workspace = null;

		return workspace;
	}

	@Override
	public void add(WorkspaceRMI workspace) throws RemoteException {

	}

	@Override
	public void update(UserRMI user, WorkspaceRMI workspace) throws RemoteException {

	}

	@Override
	public void delete(UUID workspaceID) throws RemoteException {

	}

	private WorkspaceRMI mapWorkspace(ResultSet result) {
		WorkspaceRMI workspace = new WorkspaceRMI();

		return workspace;
	}

	@Override
	public void addUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException {

	}

	@Override
	public void deleteUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException {

	}

	@Override
	public WorkspaceRMI getByItemId(Long itemId) throws RemoteException {

		WorkspaceRMI workspace = null;

		return workspace;
	}

	@Override
	public List<UserWorkspaceRMI> getMembersById(UUID workspaceId)
			throws RemoteException {
		List<UserWorkspaceRMI> users = new ArrayList<UserWorkspaceRMI>();

		return users;
	}

}
