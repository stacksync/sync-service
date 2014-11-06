package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class UserDAORMISer extends UnicastRemoteObject implements
		UserDAORMIIfc {
	
	List<UserRMI> llistat;

	public UserDAORMISer() throws RemoteException {
		llistat = new ArrayList<UserRMI>();
	}

	@Override
	public UserRMI findById(UUID userID) {
		UserRMI user = null;
		
		for (UserRMI u: llistat){
			if (u.getId() == userID){
				user = u;
			}
		}		 

		return user;
	}

	@Override
	public UserRMI getByEmail(String email) {
		UserRMI user = null;
		
		for (UserRMI u: llistat){
			if (u.getEmail().equals(email)){
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
		if (findById(user.getId()) == null){
			llistat.add(user);
			System.out.println("ADDED");
		}
	}

	@Override
	public void update(UserRMI user) {
		if (findById(user.getId()) == null){
			llistat.remove(findById(user.getId()));
			llistat.add(user);
			System.out.println("UPDATED");
		}
	}

	@Override
	public void delete(UUID userID) {
		if (findById(userID) == null){
			llistat.remove(findById(userID));
			System.out.println("DELETED");
		}
	}

	@Override
	public List<UserRMI> findByItemId(Long itemId) {
		ArrayList<UserRMI> users = new ArrayList<UserRMI>();
		/*
		 * Object[] values = { itemId };
		 * 
		 * String query = "SELECT u.* " + " FROM item i " +
		 * " INNER JOIN workspace_user wu ON i.workspace_id = wu.workspace_id "
		 * + " INNER JOIN user1 u ON wu.user_id = u.id " + " WHERE i.id = ?";
		 * 
		 * ResultSet result = null;
		 * 
		 * try { result = executeQuery(query, values);
		 * 
		 * while (result.next()) { User user = mapUser(result); users.add(user);
		 * }
		 * 
		 * } catch (SQLException e) { logger.error(e); throw new
		 * DAOException(DAOError.INTERNAL_SERVER_ERROR); }
		 */

		return users;
	}

}
