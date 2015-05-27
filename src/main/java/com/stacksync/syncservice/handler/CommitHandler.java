package com.stacksync.syncservice.handler;

import static com.stacksync.syncservice.db.DAOUtil.prepareStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.InternalServerError;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.exceptions.storage.ObjectNotFoundException;
import com.stacksync.syncservice.storage.StorageFactory;
import com.stacksync.syncservice.storage.StorageManager;
import com.stacksync.syncservice.storage.StorageManager.StorageType;
import com.stacksync.syncservice.util.Config;

public class CommitHandler {

	private static final Logger logger = Logger.getLogger(CommitHandler.class.getName());

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

	public CommitHandler(Connection connection) throws SQLException, NoStorageManagerAvailable {
		this.connection = connection;

		 String dataSource = Config.getDatasource();
		
		 DAOFactory factory = new DAOFactory(dataSource);
		
		 workspaceDAO = factory.getWorkspaceDao(connection);
		 deviceDao = factory.getDeviceDAO(connection);
		 userDao = factory.getUserDao(connection);
		 itemDao = factory.getItemDAO(connection);
		 itemVersionDao = factory.getItemVersionDAO(connection);
		 storageManager = StorageFactory.getStorageManager(StorageType.SWIFT);
	}

	// public List<CommitInfo> doCommit(User user, Workspace workspace, Device
	// device, List<ItemMetadata> items) throws DAOException {
	//
	// for (ItemMetadata itemMetadata : items) {
	// String query = "SELECT commit_object(?::uuid,";
	// List<Object> params = new ArrayList<Object>();
	// params.add(user.getId());
	//
	// query += itemMetadataToItem(itemMetadata, workspace, device, params);
	// query += itemMetadataToItemVersion(itemMetadata, workspace, device,
	// params);
	// query += itemMetadataToItemVersionChunk(itemMetadata, params);
	//
	// try {
	// executeQuery(query, params.toArray());
	// } catch (DAOException e) {
	//
	// }
	// }
	//
	// return null;
	// }
	//
	// private String itemMetadataToItem(ItemMetadata item, Workspace workspace,
	// Device device, List<Object> params) {
	//
	// params.add(item.getId());
	// params.add(workspace.getId());
	// params.add(item.getVersion());
	// params.add(item.getParentId());
	// params.add(item.getFilename());
	// params.add(item.getMimetype());
	// params.add(item.isFolder());
	// params.add(item.getParentVersion());
	//
	// return "(?,?::uuid,?,?,?,?,?,?)::item,";
	// }
	//
	// private String itemMetadataToItemVersion(ItemMetadata item, Workspace
	// workspace, Device device, List<Object> params) {
	//
	// params.add(item.getId());
	// params.add(device.getId());
	// params.add(item.getVersion());
	// params.add(item.getChecksum());
	// params.add(new java.sql.Timestamp(item.getModifiedAt().getTime()));
	// params.add(item.getStatus());
	// params.add(item.getSize());
	//
	// return "(null,?,?::uuid,?,null,?,?,?,?)::item_version,";
	// }
	//
	// private String itemMetadataToItemVersionChunk(ItemMetadata item,
	// List<Object> params) {
	//
	// String query = "ARRAY[";
	// int i = 0;
	// for (String chunkName : item.getChunks()) {
	// query += "(null,?,?)::item_version_chunk";
	// params.add(chunkName);
	// params.add(i);
	// if (i < item.getChunks().size() - 1) {
	// query += ",";
	// }
	// i++;
	// }
	//
	// query += "])";
	//
	// return query;
	//
	// }

	public List<CommitInfo> doCommit(User user, Workspace workspace, Device device, List<ItemMetadata> items) throws DAOException {

		List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();

		for (ItemMetadata itemMetadata : items) {
			String item = itemMetadataToItem(itemMetadata, workspace, device);
			String itemVersion = itemMetadataToItemVersion(itemMetadata, workspace, device);
			String itemVersionChunk = itemMetadataToItemVersionChunk(itemMetadata);

			String query = "SELECT * FROM commit_object2('" + user.getId() + "'::uuid," + item + ", " + itemVersion + ", "
					+ itemVersionChunk + ")";

			ItemMetadata objectResponse = null;
			boolean committed = true;
			try {
				ResultSet result = executeQuery(query, null);
				if (result.next()) {
					itemMetadata = DAOUtil.getItemMetadataFromResultSet(result);
				}

				objectResponse = itemMetadata;
			} catch (DAOException e) {
				if (e.getMessage().contains("Wrong version no parent")) {
					logger.info("Commit wrong version no parent");
					committed = false;
				} else {
					Item serverItem = itemDao.findById(user.getId(), itemMetadata.getId());
					objectResponse = this.getServerObjectVersion(user, serverItem, serverItem.getLatestVersion());
					if (e.getMessage().contains("Invalid version")) {
						logger.info("Commit wrong version item:" + serverItem.getId());
						committed = false;
					} else if (e.getMessage().contains("This version already exists")) {
						logger.info("Commit existant version item:" + serverItem.getId());
						committed = true;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			responseObjects.add(new CommitInfo(itemMetadata.getVersion(), committed, objectResponse));
		}

		return responseObjects;
	}

	private String itemMetadataToItem(ItemMetadata item, Workspace workspace, Device device) {

		return "(" + item.getId() + ",'" + workspace.getId() + "'::uuid," + item.getVersion() + "," + item.getParentId() + ",'"
				+ item.getFilename() + "','" + item.getMimetype() + "'," + item.isFolder() + "," + item.getParentVersion() + ")::item";

	}

	private String itemMetadataToItemVersion(ItemMetadata item, Workspace workspace, Device device) {
		return "( null," + item.getId() + ",'" + device.getId() + "'::uuid," + item.getVersion() + ",null," + item.getChecksum() + ",'"
				+ new java.sql.Timestamp(item.getModifiedAt().getTime()) + "','" + item.getStatus() + "'," + item.getSize()
				+ ")::item_version";
	}

	private String itemMetadataToItemVersionChunk(ItemMetadata item) {

		String chunkArray = "ARRAY[";
		int i = 0;
		for (String chunkName : item.getChunks()) {
			chunkArray += "(null,'" + chunkName + "'," + i + ")::item_version_chunk";
			if (i < item.getChunks().size() - 1) {
				chunkArray += ",";
			}
			i++;
		}

		chunkArray += "]";

		return chunkArray;
	}

	private ResultSet executeQuery(String query, Object[] values) throws DAOException {

		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			preparedStatement = prepareStatement(connection, query, false);
			resultSet = preparedStatement.executeQuery();

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(e, DAOError.INTERNAL_SERVER_ERROR);
		}

		return resultSet;
	}

	public Workspace doShareFolder(User user, List<String> emails, Item item, boolean isEncrypted) throws ShareProposalNotCreatedException,
			UserNotFoundException {

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
			item = itemDao.findById(user.getId(), item.getId());
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
			sourceWorkspace = workspaceDAO.getById(user.getId(), item.getWorkspace().getId());
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
					logger.error(String.format("Chunk %s not found in container %s. Could not migrate to container %s.", chunkName,
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
				logger.error(
						String.format("An error ocurred when adding the user '%s' to workspace '%s'", addressee.getId(), workspace.getId()),
						e);
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

	public UnshareData doUnshareFolder(User user, List<String> emails, Item item, boolean isEncrypted)
			throws ShareProposalNotCreatedException, UserNotFoundException {

		UnshareData response;
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
			item = itemDao.findById(user.getId(), item.getId());
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
			sourceWorkspace = workspaceDAO.getById(user.getId(), item.getWorkspace().getId());
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
				if (addressee.getId().equals(sourceWorkspace.getOwner().getId())) {
					logger.warn(String.format("Email '%s' corresponds with owner of the folder. ", email));
					throw new ShareProposalNotCreatedException("Email " + email + " corresponds with owner of the folder.");

				}

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
				// Always the last member of a shared folder should be the owner
				defaultWorkspace = workspaceDAO.getDefaultWorkspaceByUserId(sourceWorkspace.getOwner().getId());
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
					logger.error(String.format("Chunk %s not found in container %s. Could not migrate to container %s.", chunkName,
							sourceWorkspace.getSwiftContainer(), defaultWorkspace.getSwiftContainer()), e);
				} catch (Exception e) {
					logger.error(e);
					throw new ShareProposalNotCreatedException(e);
				}
			}

			// delete workspace
			try {
				workspaceDAO.delete(user.getId(), sourceWorkspace.getId());
			} catch (DAOException e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}

			// delete container from swift
			try {
				storageManager.deleteWorkspace(sourceWorkspace);
			} catch (Exception e) {
				logger.error(e);
				throw new ShareProposalNotCreatedException(e);
			}

			response = new UnshareData(usersToRemove, sourceWorkspace, true);

		} else {

			for (User userToRemove : usersToRemove) {

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
			response = new UnshareData(usersToRemove, sourceWorkspace, false);

		}
		return response;
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

	private ItemMetadata getServerObjectVersion(User user, Item serverObject, long requestedVersion) throws DAOException {

		ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(user.getId(), serverObject.getId(), requestedVersion);

		return metadata;
	}

	public Connection getConnection() {
		return this.connection;
	}

	public WorkspaceDAO getWorkspaceDAO() {
		return workspaceDAO;
	}

	public UserDAO getUserDao() {
		return userDao;
	}

	public DeviceDAO getDeviceDao() {
		return deviceDao;
	}

	public ItemDAO getItemDao() {
		return itemDao;
	}

	public ItemVersionDAO getItemVersionDao() {
		return itemVersionDao;
	}

	public StorageManager getStorageManager() {
		return storageManager;
	}

}
