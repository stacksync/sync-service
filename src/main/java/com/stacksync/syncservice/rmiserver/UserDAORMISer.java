package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class UserDAORMISer extends UnicastRemoteObject implements UserDAORMIIfc {

	List<UserRMI> llistat;

	public UserDAORMISer() throws RemoteException {
		llistat = new ArrayList<UserRMI>();
	}

	@Override
	public UserRMI findById(UUID userID) {
		UserRMI user = null;

		for (UserRMI u : llistat) {
			if (u.getId() == userID) {
				user = u;
			}
		}

		return user;
	}

	@Override
	public UserRMI getByEmail(String email) {
		UserRMI user = null;

		for (UserRMI u : llistat) {
			if (u.getEmail().equals(email)) {
				user = u;
			}
		}

		return user;
	}

	@Override
	public List<UserRMI> findAll() {

		return llistat;
	}

	@Override
	public void add(UserRMI user) {
		/*if (!user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}*/
		if (findById(user.getId()) == null) {
			llistat.add(user);
			System.out.println("ADDED");
		} else
			System.out.println("EXISTING USER ID");
	}

	@Override
	public void update(UserRMI user) {
		/*if (user.getId() == null || !user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}*/
		if (findById(user.getId()) != null) {
			llistat.remove(findById(user.getId()));
			llistat.add(user);
			System.out.println("UPDATED");
		} else
			System.out.println("USER ID DOESN'T EXIST");
	}

	@Override
	public void delete(UUID userID) {
		if (findById(userID) != null) {
			llistat.remove(findById(userID));
			System.out.println("DELETED");
		} else
			System.out.println("USER ID DOESN'T EXIST");
	}

	@Override
	public List<UserRMI> findByItemId(Long itemID) {
		ArrayList<UserRMI> users = new ArrayList<UserRMI>();

		for (UserRMI u : llistat) {
			for (WorkspaceRMI w : u.getWorkspaces()) {
				for (ItemRMI i : w.getItems()) {
					if (i.getId() == itemID) {
						users.add(u);
					}
				}
			}
		}

		return users;
	}

}
