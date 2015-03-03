package com.stacksync.syncservice.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import java.rmi.RemoteException;

public class SQLSyncHandler extends Handler implements SyncHandler {

	private static final Logger logger = Logger.getLogger(SQLSyncHandler.class.getName());	

	public SQLSyncHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable, Exception {
		super(pool);
	}

	@Override
	public List<ItemMetadata> doGetChanges(UserRMI user, WorkspaceRMI workspace) {
		List<ItemMetadata> responseObjects = new ArrayList<ItemMetadata>();

            try {
                responseObjects = itemDao.getItemsByWorkspaceId(workspace.getId());
            } catch (RemoteException ex) {
                logger.error(ex);
            }

		return responseObjects;
	}

	@Override
	public List<WorkspaceRMI> doGetWorkspaces(UserRMI user) throws NoWorkspacesFoundException {

		List<WorkspaceRMI> workspaces = new ArrayList<WorkspaceRMI>();

		try {
			workspaces = workspaceDAO.getByUserId(user.getId());

		} catch (RemoteException ex) {
                logger.error(ex);
            }

		return workspaces;
	}

	@Override
	public UUID doUpdateDevice(DeviceRMI device) throws UserNotFoundException, DeviceNotValidException,
			DeviceNotUpdatedException {

		try {
			if (device.getId() == null) {
				deviceDao.add(device);
			} else {
				deviceDao.update(device);
			}
		} catch (IllegalArgumentException e) {
			logger.error(e);
			throw new DeviceNotValidException(e);
		} catch (RemoteException ex) {
                        logger.error(ex);
            }

		return device.getId();
	}
	

	@Override
	public void doUpdateWorkspace(UserRMI user, WorkspaceRMI workspace) throws UserNotFoundException,
			WorkspaceNotUpdatedException {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                logger.error(ex);
            }

		// Update the workspace
		try {
			workspaceDAO.update(user, workspace);
		} catch (RemoteException ex) {
                logger.error(ex);
            }
	}

	@Override
	public UserRMI doGetUser(String email) throws UserNotFoundException {

            try {
                UserRMI user = userDao.getByEmail(email);
                return user;
            } catch (RemoteException ex) {
                logger.error(ex);
            }
            return null;
	}

    @Override
    public Connection getConnection() {
        return super.getConnection();
    }

}
