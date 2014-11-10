package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class WorkspaceDAORMISer extends UnicastRemoteObject implements
		WorkspaceDAORMIIfc {

	List<WorkspaceRMI> llistat;
	List<UserWorkspaceRMI> llistatuw;

	public WorkspaceDAORMISer() throws RemoteException {
		llistat = new ArrayList<WorkspaceRMI>();
		llistatuw = new ArrayList<UserWorkspaceRMI>();
	}

	@Override
	public WorkspaceRMI getById(UUID workspaceID) throws RemoteException {
		WorkspaceRMI workspace = null;

		for (WorkspaceRMI w : llistat) {
			if (w.getId() == workspaceID) {
				for (UserWorkspaceRMI uw : llistatuw) {
					if (uw.getWorkspace().getId() == workspaceID) {
						workspace = w;
					}
				}
			}
		}

		return workspace;
	}

	@Override
	public List<WorkspaceRMI> getByUserId(UUID userID) throws RemoteException {

		List<WorkspaceRMI> workspaces = new ArrayList<WorkspaceRMI>();

		for (WorkspaceRMI w : llistat) {
			for (UserRMI u : w.getUsers()) {
				if (u.getId() == userID) {
					for (UserWorkspaceRMI uw : llistatuw) {
						if (uw.getWorkspace().getId() == w.getId()) {
							workspaces.add(w);
						}
					}
				}
			}
		}

		return workspaces;
	}

	@Override
	public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userID)
			throws RemoteException {
		WorkspaceRMI workspace = null;

		for (WorkspaceRMI w : llistat) {
			if (userID == w.getOwner().getId() && !w.isShared()) {
				for (UserWorkspaceRMI uw : llistatuw) {
					if (uw.getWorkspace().getId() == w.getId()) {
						workspace = w;
					}
				}
			}
		}

		return workspace;
	}

	@Override
	public void add(WorkspaceRMI workspace) throws RemoteException {
		if (!workspace.isValid()) {
			throw new IllegalArgumentException("Workspace attributes not set");
		}
		if (getById(workspace.getId()) == null) {
			llistat.add(workspace);
		} else
			System.out.println("EXISTING WORKSPACE ID");
	}

	@Override
	public void update(UserRMI user, WorkspaceRMI workspace)
			throws RemoteException {
		if (workspace.getId() == null || user.getId() == null) {
			throw new IllegalArgumentException("Attributes not set");
		}

		if (getById(user.getId()) != null) {
			llistat.remove(getById(user.getId()));
			llistat.add(workspace);
			System.out.println("UPDATED");
		} else
			System.out.println("WORKSPACE DOESN'T EXIST");
	}

	@Override
	public void delete(UUID workspaceID) throws RemoteException {
		if (getById(workspaceID) != null) {
			llistat.remove(getById(workspaceID));
			System.out.println("DELETED");
		} else
			System.out.println("WORKSPACE DOESN'T EXIST");
	}

	@Override
	public void addUser(UserRMI user, WorkspaceRMI workspace)
			throws RemoteException {
		boolean exist = false;

		if (user == null || !user.isValid()) {
			throw new IllegalArgumentException("User not valid");
		} else if (workspace == null || !workspace.isValid()) {
			throw new IllegalArgumentException("Workspace not valid");
		}

		for (UserRMI u : workspace.getUsers()) {
			if (u.equals(user)) {
				exist = true;
			}
		}

		if (!exist) {
			workspace.getUsers().add(user);
			System.out.println("USER ADDED");
		} else
			System.out.println("USER DOES ALREDY EXIST");
	}

	@Override
	public void deleteUser(UserRMI user, WorkspaceRMI workspace)
			throws RemoteException {
		boolean exist = false;

		if (user == null || !user.isValid()) {
			throw new IllegalArgumentException("User not valid");
		} else if (workspace == null || !workspace.isValid()) {
			throw new IllegalArgumentException("Workspace not valid");
		}

		for (UserRMI u : workspace.getUsers()) {
			if (u.equals(user)) {
				exist = true;
			}
		}

		if (exist) {
			workspace.getUsers().remove(user);
			System.out.println("USER DELETED");
		} else
			System.out.println("USER DOESN'T EXIST");
	}

	@Override
	public WorkspaceRMI getByItemId(Long itemID) throws RemoteException {

		WorkspaceRMI workspace = null;

		for (WorkspaceRMI w : llistat) {
			for (ItemRMI i : w.getItems()) {
				if (i.getId() == itemID) {
					for (UserWorkspaceRMI uw : llistatuw) {
						if (uw.getWorkspace().getId() == w.getId()) {
							workspace = w;
						}
					}
				}
			}
		}

		return workspace;
	}

	@Override
	public List<UserWorkspaceRMI> getMembersById(UUID workspaceID)
			throws RemoteException {
		List<UserWorkspaceRMI> users = new ArrayList<UserWorkspaceRMI>();
		
		for (WorkspaceRMI w : llistat) {
			if (w.getId() == workspaceID) {
				for (UserWorkspaceRMI uw : llistatuw) {
					if (uw.getWorkspace().getId() == w.getId()) {
						for (UserRMI u : w.getUsers()){
							if (w.getOwner().equals(u)){
								users.add(uw);
							}
						}
					}
				}
			}
		}

		return users;
	}
}
