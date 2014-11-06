package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
import com.stacksync.syncservice.exceptions.InternalServerError;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.exceptions.storage.ObjectNotFoundException;
import com.stacksync.syncservice.storage.StorageFactory;
import com.stacksync.syncservice.storage.StorageManager;
import com.stacksync.syncservice.storage.StorageManager.StorageType;
import com.stacksync.syncservice.util.Config;

public class Handler {

	private static final Logger logger = Logger.getLogger(Handler.class.getName());

	protected Connection connection;
	protected WorkspaceDAO workspaceDAO;
	protected UserDAO userDao;
	protected DeviceDAO deviceDao;
	protected ItemDAO itemDao;
	protected ItemVersionDAO itemVersionDao;

	protected StorageManager storageManager;

	public enum Status {
		NEW, DELETED, CHANGED, RENAMED, MOVED
	};

	public Handler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
		connection = pool.getConnection();

		String dataSource = Config.getDatasource();

		DAOFactory factory = new DAOFactory(dataSource);

		workspaceDAO = factory.getWorkspaceDao(connection);
		deviceDao = factory.getDeviceDAO(connection);
		userDao = factory.getUserDao(connection);
		itemDao = factory.getItemDAO(connection);
		itemVersionDao = factory.getItemVersionDAO(connection);
		storageManager = StorageFactory.getStorageManager(StorageType.SWIFT);
	}

	public List<CommitInfo> doCommit(User user, Workspace workspace, Device device, List<ItemMetadata> items)
			throws DAOException {

		HashMap<Long, Long> tempIds = new HashMap<Long, Long>();

		workspace = workspaceDAO.getById(workspace.getId());
		// TODO: check if the workspace belongs to the user or its been given
		// access

		device = deviceDao.get(device.getId());
		// TODO: check if the device belongs to the user

		List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();

		for (ItemMetadata item : items) {

			ItemMetadata objectResponse = null;
			boolean committed;

			try {

				if (item.getParentId() != null) {
					Long parentId = tempIds.get(item.getParentId());
					if (parentId != null) {
						item.setParentId(parentId);
					}
				}

				// if the item does not have ID but has a TempID, maybe it was
				// set
				if (item.getId() == null && item.getTempId() != null) {
					Long newId = tempIds.get(item.getTempId());
					if (newId != null) {
						item.setId(newId);
					}
				}

				this.commitObject(item, workspace, device);

				if (item.getTempId() != null) {
					tempIds.put(item.getTempId(), item.getId());
				}

				objectResponse = item;
				committed = true;
			} catch (CommitWrongVersion e) {
				Item serverObject = e.getItem();
				objectResponse = this.getCurrentServerVersion(serverObject);
				committed = false;
			} catch (CommitWrongVersionNoParent e) {
				committed = false;
			} catch (CommitExistantVersion e) {
				Item serverObject = e.getItem();
				objectResponse = this.getCurrentServerVersion(serverObject);
				committed = true;
			}

			responseObjects.add(new CommitInfo(item.getVersion(), committed, objectResponse));
		}

		return responseObjects;
	}

	public Workspace doShareFolder(User user, List<String> emails, Item item, boolean isEncrypted)
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

		// Get folder metadata
		try {
			item = itemDao.findById(item.getId());
		} catch (DAOException e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}

		if (item == null || !item.isFolder()) {
			throw new ShareProposalNotCreatedException("No folder found with the given ID.");
		}

		// Get the source workspace
		Workspace sourceWorkspace;
		try {
			sourceWorkspace = workspaceDAO.getById(item.getWorkspace().getId());
		} catch (DAOException e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}
		if (sourceWorkspace == null) {
			throw new ShareProposalNotCreatedException("Workspace not found.");
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

		Workspace workspace;

		if (sourceWorkspace.isShared()) {
			workspace = sourceWorkspace;

		} else {
			// Create the new workspace
			String container = UUID.randomUUID().toString();

			workspace = new Workspace();
			workspace.setShared(true);
			workspace.setEncrypted(isEncrypted);
			workspace.setName(item.getFilename());
			workspace.setOwner(user);
			workspace.setUsers(addressees);
			workspace.setSwiftContainer(container);
			workspace.setSwiftUrl(Config.getSwiftUrl() + "/" + user.getSwiftAccount());

			// Create container in Swift
			try {
				storageManager.createNewWorkspace(workspace);
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

			// Migrate files to new workspace
			List<String> chunks;
			try {
				chunks = itemDao.migrateItem(item.getId(), workspace.getId());
			} catch (Exception e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}

			// Move chunks to new container
			for (String chunkName : chunks) {
				try {
					storageManager.copyChunk(sourceWorkspace, workspace, chunkName);
				} catch (ObjectNotFoundException e) {
					logger.error(String.format(
							"Chunk %s not found in container %s. Could not migrate to container %s.", chunkName,
							sourceWorkspace.getSwiftContainer(), workspace.getSwiftContainer()), e);
				} catch (Exception e) {
					logger.error(e);
					throw new ShareProposalNotCreatedException(e);
				}
			}
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

	public void doUnshareFolder(User user, List<String> emails, Item item, boolean isEncrypted)
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

		// Get folder metadata
		try {
			item = itemDao.findById(item.getId());
		} catch (DAOException e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}

		if (item == null || !item.isFolder()) {
			throw new ShareProposalNotCreatedException("No folder found with the given ID.");
		}

		// Get the workspace
		Workspace sourceWorkspace;
		try {
			sourceWorkspace = workspaceDAO.getById(item.getWorkspace().getId());
		} catch (DAOException e) {
			logger.error(e);
			throw new ShareProposalNotCreatedException(e);
		}
		if (sourceWorkspace == null) {
			throw new ShareProposalNotCreatedException("Workspace not found.");
		}
		if (!sourceWorkspace.isShared()) {
			throw new ShareProposalNotCreatedException("This workspace is not shared.");
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

		// get workspace members
		List<UserWorkspace> workspaceMembers;
		try {
			workspaceMembers = doGetWorkspaceMembers(user, sourceWorkspace);
		} catch (InternalServerError e1) {
			throw new ShareProposalNotCreatedException(e1.toString());
		}

		// remove users from workspace
		List<User> usersToRemove = new ArrayList<User>();
		
		for (User userToRemove : addressees) {
			for (UserWorkspace member : workspaceMembers) {
				if (member.getUser().getEmail().equals(userToRemove.getEmail())) {
					workspaceMembers.remove(member);
					usersToRemove.add(userToRemove);
					break;
				}
			}
		}

		if (workspaceMembers.size() <= 1) {
			// All members have been removed from the workspace

			Workspace defaultWorkspace;
			try {
				defaultWorkspace = workspaceDAO.getDefaultWorkspaceByUserId(user.getId());
			} catch (DAOException e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException("Could not get default workspace");
			}

			// Migrate files to new workspace
			List<String> chunks;
			try {
				chunks = itemDao.migrateItem(item.getId(), defaultWorkspace.getId());
			} catch (Exception e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}

			// Move chunks to new container
			for (String chunkName : chunks) {
				try {
					storageManager.copyChunk(sourceWorkspace, defaultWorkspace, chunkName);
				} catch (ObjectNotFoundException e) {
					logger.error(String.format(
							"Chunk %s not found in container %s. Could not migrate to container %s.", chunkName,
							sourceWorkspace.getSwiftContainer(), defaultWorkspace.getSwiftContainer()), e);
				} catch (Exception e) {
					logger.error(e);
					throw new ShareProposalNotCreatedException(e);
				}
			}
			
			// delete workspace
			try {
				workspaceDAO.delete(sourceWorkspace.getId());
			} catch (DAOException e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}
			
			// delete container from swift
			try {
				storageManager.deleteWorkspace(defaultWorkspace);
			} catch (Exception e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}

		} else {
			
			for(User userToRemove : usersToRemove){
				
				try {
					workspaceDAO.deleteUser(userToRemove, sourceWorkspace);
				} catch (DAOException e) {
					logger.error(e);
					throw new ShareProposalNotCreatedException(e);
				}
				
				try {
					storageManager.removeUserToWorkspace(user, userToRemove, sourceWorkspace);
				} catch (Exception e) {
					logger.error(e);
					throw new ShareProposalNotCreatedException(e);
				}
			}
		}
	}

	public List<UserWorkspace> doGetWorkspaceMembers(User user, Workspace workspace) throws InternalServerError {

		// TODO: check user permissions.

		List<UserWorkspace> members;
		try {
			members = workspaceDAO.getMembersById(workspace.getId());

		} catch (DAOException e) {
			logger.error(e);
			throw new InternalServerError(e);
		}

		if (members == null || members.isEmpty()) {
			throw new InternalServerError("No members found in workspace.");
		}

		return members;
	}

	public Connection getConnection() {
		return this.connection;
	}

	/*
	 * Private functions
	 */

	private void commitObject(ItemMetadata item, Workspace workspace, Device device) throws CommitWrongVersionNoParent,
			CommitWrongVersion, CommitExistantVersion, DAOException {

		Item serverItem = itemDao.findById(item.getId());

		// Check if this object already exists in the server.
		if (serverItem == null) {
			if (item.getVersion() == 1) {
				this.saveNewObject(item, workspace, device);
			} else {
				throw new CommitWrongVersionNoParent();
			}
			return;
		}

		// Check if the client version already exists in the server
		long serverVersion = serverItem.getLatestVersion();
		long clientVersion = item.getVersion();
		boolean existVersionInServer = (serverVersion >= clientVersion);

		if (existVersionInServer) {
			this.saveExistentVersion(serverItem, item);
		} else {
			// Check if version is correct
			if (serverVersion + 1 == clientVersion) {
				this.saveNewVersion(item, serverItem, workspace, device);
			} else {
				throw new CommitWrongVersion("Invalid version.", serverItem);
			}
		}
	}

	private void saveNewObject(ItemMetadata metadata, Workspace workspace, Device device) throws DAOException {
		// Create workspace and parent instances
		Long parentId = metadata.getParentId();
		Item parent = null;
		if (parentId != null) {
			parent = itemDao.findById(parentId);
		}

		beginTransaction();

		try {
			// Insert object to DB

			Item item = new Item();
			item.setId(metadata.getId());
			item.setFilename(metadata.getFilename());
			item.setMimetype(metadata.getMimetype());
			item.setIsFolder(metadata.isFolder());
			item.setClientParentFileVersion(metadata.getParentVersion());

			item.setLatestVersion(metadata.getVersion());
			item.setWorkspace(workspace);
			item.setParent(parent);

			itemDao.put(item);

			// set the global ID
			metadata.setId(item.getId());

			// Insert objectVersion
			ItemVersion objectVersion = new ItemVersion();
			objectVersion.setVersion(metadata.getVersion());
			objectVersion.setModifiedAt(metadata.getModifiedAt());
			objectVersion.setChecksum(metadata.getChecksum());
			objectVersion.setStatus(metadata.getStatus());
			objectVersion.setSize(metadata.getSize());

			objectVersion.setItem(item);
			objectVersion.setDevice(device);
			itemVersionDao.add(objectVersion);

			// If no folder, create new chunks
			if (!metadata.isFolder()) {
				List<String> chunks = metadata.getChunks();
				this.createChunks(chunks, objectVersion);
			}

			commitTransaction();
		} catch (Exception e) {
			logger.error(e);
			rollbackTransaction();
		}
	}

	private void saveNewVersion(ItemMetadata metadata, Item serverItem, Workspace workspace, Device device)
			throws DAOException {

		beginTransaction();

		try {
			// Create new objectVersion
			ItemVersion itemVersion = new ItemVersion();
			itemVersion.setVersion(metadata.getVersion());
			itemVersion.setModifiedAt(metadata.getModifiedAt());
			itemVersion.setChecksum(metadata.getChecksum());
			itemVersion.setStatus(metadata.getStatus());
			itemVersion.setSize(metadata.getSize());

			itemVersion.setItem(serverItem);
			itemVersion.setDevice(device);

			itemVersionDao.add(itemVersion);

			// If no folder, create new chunks
			if (!metadata.isFolder()) {
				List<String> chunks = metadata.getChunks();
				this.createChunks(chunks, itemVersion);
			}

			// TODO To Test!!
			String status = metadata.getStatus();
			if (status.equals(Status.RENAMED.toString()) || status.equals(Status.MOVED.toString())
					|| status.equals(Status.DELETED.toString())) {

				serverItem.setFilename(metadata.getFilename());

				Long parentFileId = metadata.getParentId();
				if (parentFileId == null) {
					serverItem.setClientParentFileVersion(null);
					serverItem.setParent(null);
				} else {
					serverItem.setClientParentFileVersion(metadata.getParentVersion());
					Item parent = itemDao.findById(parentFileId);
					serverItem.setParent(parent);
				}
			}

			// Update object latest version
			serverItem.setLatestVersion(metadata.getVersion());
			itemDao.put(serverItem);

			commitTransaction();
		} catch (Exception e) {
			logger.error(e);
			rollbackTransaction();
		}
	}

	private void createChunks(List<String> chunksString, ItemVersion objectVersion) throws IllegalArgumentException,
			DAOException {
		if (chunksString != null) {
			if (chunksString.size() > 0) {
				List<Chunk> chunks = new ArrayList<Chunk>();
				int i = 0;

				for (String chunkName : chunksString) {
					chunks.add(new Chunk(chunkName, i));
					i++;
				}

				itemVersionDao.insertChunks(chunks, objectVersion.getId());
			}
		}
	}

	private void saveExistentVersion(Item serverObject, ItemMetadata clientMetadata) throws CommitWrongVersion,
			CommitExistantVersion, DAOException {

		ItemMetadata serverMetadata = this.getServerObjectVersion(serverObject, clientMetadata.getVersion());

		if (!clientMetadata.equals(serverMetadata)) {
			throw new CommitWrongVersion("Invalid version.", serverObject);
		}

		boolean lastVersion = (serverObject.getLatestVersion().equals(clientMetadata.getVersion()));

		if (!lastVersion) {
			throw new CommitExistantVersion("This version already exists.", serverObject, clientMetadata.getVersion());
		}
	}

	private ItemMetadata getCurrentServerVersion(Item serverObject) throws DAOException {
		return getServerObjectVersion(serverObject, serverObject.getLatestVersion());
	}

	private ItemMetadata getServerObjectVersion(Item serverObject, long requestedVersion) throws DAOException {

		ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(serverObject.getId(), requestedVersion);

		return metadata;
	}

	private void beginTransaction() throws DAOException {
		try {
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	private void commitTransaction() throws DAOException {
		try {
			connection.commit();
			this.connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	private void rollbackTransaction() throws DAOException {
		try {
			this.connection.rollback();
			this.connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

}