package com.stacksync.syncservice.handler;

import com.stacksync.commons.exceptions.*;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLSyncHandler extends Handler implements SyncHandler {

	private static final Logger logger = Logger.getLogger(SQLSyncHandler.class.getName());	

	public SQLSyncHandler(ConnectionPool pool) throws Exception {
		super(pool);
	}

	@Override
	public List<ItemMetadataRMI> doGetChanges(UserRMI user, WorkspaceRMI workspace) {

		List<ItemMetadataRMI> responseObjects = new ArrayList<>();

		try {
			responseObjects = globalDAO.getItemsByWorkspaceId(workspace.getId());
		} catch (RemoteException ex) {
			logger.error(ex);
		}

		return responseObjects;
	}

	@Override
	public List<WorkspaceRMI> doGetWorkspaces(UserRMI user) throws NoWorkspacesFoundException {

		List<WorkspaceRMI> workspaces = new ArrayList<WorkspaceRMI>();

		try {
			workspaces = globalDAO.getByUserId(user.getId());

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
				globalDAO.add(device);
			} else {
				globalDAO.update(device);
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
			user = globalDAO.findById(user.getId());
		} catch (RemoteException ex) {
                logger.error(ex);
            }

		// Update the workspace
		try {
			globalDAO.update(user, workspace);
		} catch (RemoteException ex) {
                logger.error(ex);
            }
	}

	@Override
	public UserRMI doGetUser(String email) throws UserNotFoundException {

            try {
                UserRMI user = globalDAO.getByEmail(email);
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

	@Override
	public void createUser(UUID id) throws Exception{
		populate(id);
	}


}
