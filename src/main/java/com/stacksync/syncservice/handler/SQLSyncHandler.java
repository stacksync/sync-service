package com.stacksync.syncservice.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;
import com.stacksync.syncservice.exceptions.dao.NoRowsAffectedDAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.storage.StorageFactory;
import com.stacksync.syncservice.storage.StorageManager;
import com.stacksync.syncservice.storage.StorageManager.StorageType;
import com.stacksync.syncservice.util.Config;

public class SQLSyncHandler extends Handler implements SyncHandler {

	private static final Logger logger = Logger.getLogger(SQLSyncHandler.class.getName());
	private StorageManager storageManager;

	public SQLSyncHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
		super(pool);
		storageManager = StorageFactory.getStorageManager(StorageType.SWIFT);
	}

	@Override
	public List<ItemMetadata> doGetChanges(User user, Workspace workspace) {
		List<ItemMetadata> responseObjects = new ArrayList<ItemMetadata>();

		try {
			responseObjects = itemDao.getItemsByWorkspaceId(workspace.getId());
		} catch (DAOException e) {
			logger.error(e.toString(), e);
		}

		return responseObjects;
	}

	@Override
	public List<Workspace> doGetWorkspaces(User user) throws NoWorkspacesFoundException {

		List<Workspace> workspaces = new ArrayList<Workspace>();

		try {
			workspaces = workspaceDAO.getByUserId(user.getId());

		} catch (NoResultReturnedDAOException e) {
			logger.error(e);
			throw new NoWorkspacesFoundException(String.format("No workspaces found for user: %s", user.getId()));
		} catch (DAOException e) {
			logger.error(e);
			throw new NoWorkspacesFoundException(e);
		}

		return workspaces;
	}

	@Override
	public UUID doUpdateDevice(Device device) throws UserNotFoundException, DeviceNotValidException,
			DeviceNotUpdatedException {

		try {
			User dbUser = userDao.findById(device.getUser().getId());
			device.setUser(dbUser);

		} catch (NoResultReturnedDAOException e) {
			logger.warn(e);
			throw new UserNotFoundException(e);
		} catch (DAOException e) {
			logger.error(e);
			throw new DeviceNotUpdatedException(e);
		}

		try {
			if (device.getId() == null) {
				deviceDao.add(device);
			} else {
				deviceDao.update(device);
			}
		} catch (NoRowsAffectedDAOException e) {
			logger.error(e);
			throw new DeviceNotUpdatedException(e);
		} catch (DAOException e) {
			logger.error(e);
			throw new DeviceNotUpdatedException(e);
		} catch (IllegalArgumentException e) {
			logger.error(e);
			throw new DeviceNotValidException(e);
		}

		return device.getId();
	}

	@Override
	public Workspace doCreateShareProposal(User user, List<String> emails, String folderName, boolean isEncrypted)
			throws ShareProposalNotCreatedException, UserNotFoundException {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (NoResultReturnedDAOException e) {
			logger.warn(e);
			throw new UserNotFoundException(e);
		} catch (DAOException e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}

		// Check the addressees
		List<User> addressees = new ArrayList<User>();
		for (String email : emails) {
			User addressee;
			try {
				addressee = userDao.getByEmail(email);
				if (!addressee.getId().equals(user.getId())) {
					addressees.add(addressee);
				}

			} catch (IllegalArgumentException e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			} catch (DAOException e) {
				logger.warn(String.format("Email '%s' does not correspond with any user. ", email), e);
			}
		}

		if (addressees.isEmpty()) {
			throw new ShareProposalNotCreatedException("No addressees found");
		}

		// Create the new workspace
		String container = UUID.randomUUID().toString();

		Workspace workspace = new Workspace();
		workspace.setShared(true);
		workspace.setEncrypted(isEncrypted);
		workspace.setName(folderName);
		workspace.setOwner(user);
		workspace.setUsers(addressees);
		workspace.setSwiftContainer(container);
		workspace.setSwiftUrl(Config.getSwiftUrl() + "/" + user.getSwiftAccount());

		// Create container in Swift
		try {
			storageManager.createNewWorkspace(user, workspace);
		} catch (Exception e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}

		// Save the workspace to the DB
		try {
			workspaceDAO.add(workspace);
			// add the owner to the workspace
			workspaceDAO.addUser(user, workspace);

		} catch (DAOException e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}

		// Grant user to container in Swift
		try {
			storageManager.grantUserToWorkspace(user, user, workspace);
		} catch (Exception e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}

		// Add the addressees to the workspace
		for (User addressee : addressees) {
			try {
				workspaceDAO.addUser(addressee, workspace);

			} catch (DAOException e) {
				workspace.getUsers().remove(addressee);
				logger.error(String.format("An error ocurred when adding the user '%s' to workspace '%s'",
						addressee.getId(), workspace.getId()), e);
			}

			// Grant the user to container in Swift
			try {
				storageManager.grantUserToWorkspace(user, addressee, workspace);
			} catch (Exception e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}
		}

		return workspace;
	}

	@Override
	public void doUpdateWorkspace(User user, Workspace workspace) throws UserNotFoundException,
			WorkspaceNotUpdatedException {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (NoResultReturnedDAOException e) {
			logger.warn(e);
			throw new UserNotFoundException(e);
		} catch (DAOException e) {
			logger.error(e);
			throw new WorkspaceNotUpdatedException(e);
		}

		// Update the workspace
		try {
			workspaceDAO.update(user, workspace);
		} catch (NoRowsAffectedDAOException e) {
			logger.error(e);
			throw new WorkspaceNotUpdatedException(e);
		} catch (DAOException e) {
			logger.error(e);
			throw new WorkspaceNotUpdatedException(e);
		}
	}

	@Override
	public User doGetUser(String email) throws UserNotFoundException {

		try {
			User user = userDao.getByEmail(email);
			return user;

		} catch (NoResultReturnedDAOException e) {
			logger.error(e);
			throw new UserNotFoundException(e);
		} catch (DAOException e) {
			logger.error(e);
			throw new UserNotFoundException(e);
		}
	}

}
