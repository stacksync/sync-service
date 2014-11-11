package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class UserDAORMISer extends UnicastRemoteObject implements UserDAORMIIfc {

	List<UserRMI> list;

	public UserDAORMISer() throws RemoteException {
		list = new ArrayList<UserRMI>();
	}

	@Override
	public UserRMI findById(UUID userID) {
		UserRMI user = null;

		for (UserRMI u : list) {
			if (u.getId().equals(userID)) {
				user = u;
			}
		}

		return user;
	}

	@Override
	public UserRMI getByEmail(String email) {
		UserRMI user = null;

		for (UserRMI u : list) {
			if (u.getEmail().equals(email)) {
				user = u;
			}
		}

		return user;
	}

	@Override
	public List<UserRMI> findAll() {

		return list;
	}

	@Override
	public void add(UserRMI user) {
		if (!user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}
		if (findById(user.getId()) == null) {
			list.add(user);
			System.out.println("ADDED");
		} else
			System.out.println("EXISTING USER ID");
	}

	@Override
	public void update(UserRMI user) {
		if (user.getId() == null || !user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}
		if (findById(user.getId()) != null) {
			list.remove(findById(user.getId()));
			list.add(user);
			System.out.println("UPDATED");
		} else
			System.out.println("USER ID DOESN'T EXIST");
	}

	@Override
	public void delete(UUID userID) {
		if (findById(userID) != null) {
			list.remove(findById(userID));
			System.out.println("DELETED");
		} else
			System.out.println("USER ID DOESN'T EXIST");
	}

	@Override
	public List<UserRMI> findByItemId(Long itemID) {
		ArrayList<UserRMI> users = new ArrayList<UserRMI>();

		for (UserRMI u : list) {
			for (WorkspaceRMI w : u.getWorkspaces()) {
				for (ItemRMI i : w.getItems()) {
					if (i.getId().equals(itemID)) {
						users.add(u);
					}
				}
			}
		}

		return users;
	}

}
