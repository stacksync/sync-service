package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
//import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//import org.apache.log4j.Logger;

//import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;
//import com.stacksync.syncservice.db.DAOError;
//import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;
//import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;

public class WorkspaceDAORMISer extends UnicastRemoteObject implements
		WorkspaceDAORMIIfc {

	// private static final Logger logger =
	// Logger.getLogger(PostgresqlWorkspaceDAO.class.getName());

	public WorkspaceDAORMISer() throws RemoteException {
		super();
	}

	@Override
	public Workspace getById(UUID workspaceID) throws DAOException {
		Workspace workspace = null;

		return workspace;
	}

	@Override
	public List<Workspace> getByUserId(UUID userId) throws DAOException {

		List<Workspace> workspaces = new ArrayList<Workspace>();

		return workspaces;
	}

	@Override
	public Workspace getDefaultWorkspaceByUserId(UUID userId)
			throws DAOException {

		Workspace workspace = null;

		return workspace;
	}

	@Override
	public void add(Workspace workspace) throws DAOException {

	}

	@Override
	public void update(User user, Workspace workspace) throws DAOException {

	}

	@Override
	public void delete(UUID workspaceID) throws DAOException {

	}

	private Workspace mapWorkspace(ResultSet result) throws SQLException {
		Workspace workspace = new Workspace();

		return workspace;
	}

	@Override
	public void addUser(User user, Workspace workspace) throws DAOException {

	}

	@Override
	public void deleteUser(User user, Workspace workspace) throws DAOException {

	}

	@Override
	public Workspace getByItemId(Long itemId) throws DAOException {

		Workspace workspace = null;

		return workspace;
	}

	@Override
	public List<UserWorkspace> getMembersById(UUID workspaceId)
			throws DAOException {
		List<UserWorkspace> users = new ArrayList<UserWorkspace>();

		return users;
	}

}
