package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
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
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIUserMetadata;
import com.stacksync.syncservice.storage.StorageFactory;
import com.stacksync.syncservice.storage.StorageManager;
import com.stacksync.syncservice.storage.StorageManager.StorageType;
import com.stacksync.syncservice.util.Config;
import com.stacksync.syncservice.util.Constants;

public class SQLHandler implements Handler {

	private static final Logger logger = Logger.getLogger(SQLHandler.class.getName());

	private Connection connection;
	private StorageManager storageManager;
	private WorkspaceDAO workspaceDAO;
	private UserDAO userDao;
	private DeviceDAO deviceDao;
	private ItemDAO itemDao;
	private ItemVersionDAO itemVersionDao;

	private Device apiDevice = new Device(Constants.API_DEVICE_ID);

	public SQLHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
		connection = pool.getConnection();

		String dataSource = Config.getDatasource();

		DAOFactory factory = new DAOFactory(dataSource);

		storageManager = StorageFactory.getStorageManager(StorageType.SWIFT);

		workspaceDAO = factory.getWorkspaceDao(connection);
		userDao = factory.getUserDao(connection);
		deviceDao = factory.getDeviceDAO(connection);
		itemDao = factory.getItemDAO(connection);
		itemVersionDao = factory.getItemVersionDAO(connection);
	}

	@Override
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
	public APIGetMetadata ApiGetMetadata(User user, Long fileId, Boolean includeList, Boolean includeDeleted,
			Boolean includeChunks, Long version) {

		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (fileId == null) {
				// retrieve metadata from the root folder
				responseObject = this.itemDao.findByUserId(user.getId(), includeDeleted);

			} else {

				// check if user has permission on this file
				List<User> users = this.userDao.findByItemId(fileId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.itemDao.findById(fileId, includeList, version, includeDeleted, includeChunks);
			}

			success = true;

		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		}

		APIGetMetadata response = new APIGetMetadata(responseObject, success, errorCode, description);
		return response;
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

	@Override
	public APICommitResponse ApiCommitMetadata(User user, Boolean overwrite, ItemMetadata fileToSave,
			ItemMetadata parentMetadata) {
		List<ItemMetadata> files = parentMetadata.getChildren();

		ItemMetadata fileToModify = null;
		for (ItemMetadata file : files) {
			if (file.getFilename().equals(fileToSave.getFilename())) {
				fileToModify = file;
				break;
			}
		}

		ItemMetadata object = null;
		if (fileToModify == null) {
			object = saveNewItemAPI(user, null, fileToSave, parentMetadata);
		} else {
			if (overwrite) {
				object = saveNewVersionAPI(user, fileToSave, fileToModify);
			} else {
				/*
				 * TODO Create conflict copy
				 */
			}
		}

		APICommitResponse responseAPI = new APICommitResponse(object, true, 0, "");
		return responseAPI;
	}

	@Override
	public APICreateFolderResponse ApiCreateFolder(User user, ItemMetadata item) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			APICreateFolderResponse response = new APICreateFolderResponse(item, false, 404,
					"User not found.");
			return response;
		}

		// get metadata of the parent item
		APIGetMetadata parentResponse = ApiGetMetadata(user, item.getParentId(), true, true, false, null);
		ItemMetadata parentMetadata = parentResponse.getItemMetadata();
		
		// if it is the root, get the default workspace
		
		
		if (parentMetadata.isRoot()){
			
			try {
				Workspace workspace = workspaceDAO.getDefaultWorkspaceByUserId(user.getId());
				parentMetadata.setWorkspaceId(workspace.getId());
			} catch (DAOException e) {
				logger.error(e);
				APICreateFolderResponse response = new APICreateFolderResponse(item, false, 404,
						"Workspace not found.");
				return response;
			}
		}

		String folderName = item.getFilename();
		List<ItemMetadata> files = parentMetadata.getChildren();

		// check if there exists a folder with the same name
		ItemMetadata object = null;
		for (ItemMetadata file : files) {
			if (file.getFilename().equals(folderName) && !file.getStatus().equals("DELETED")) {
				object = file;
				break;
			}
		}

		if (object != null) {
			APICreateFolderResponse response = new APICreateFolderResponse(object, false, 400, "Folder already exists.");
			return response;
		}

		boolean succeded = this.createNewFolder(user, item, parentMetadata);

		if (!succeded) {
			APICreateFolderResponse response = new APICreateFolderResponse(item, false, 500,
					"Item could not be committed.");
			return response;
		}

		APICreateFolderResponse responseAPI = new APICreateFolderResponse(item, true, 0, "");
		return responseAPI;
	}

	@Override
	public APIRestoreMetadata ApiRestoreMetadata(User user, ItemMetadata item) {
		try {

			Item serverItem = itemDao.findById(item.getId());
			ItemMetadata lastObjectVersion = itemDao.findById(item.getId(), false, null, false, false);
			if (serverItem != null && lastObjectVersion != null) {

				ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(serverItem.getId(), item.getVersion());

				ItemVersion restoredObject = new ItemVersion(metadata);

				if (restoredObject != null && restoredObject.getStatus().compareTo(Status.DELETED.toString()) != 0) {
					restoredObject.setVersion(lastObjectVersion.getVersion() + 1);
					restoredObject.setStatus(Status.CHANGED.toString());

					// save restoredObject
					itemVersionDao.add(restoredObject);

					List<String> chunks = new ArrayList<String>();
					// If no folder, create new chunks
					if (!restoredObject.getChunks().isEmpty()) {
						for (Chunk chunk : restoredObject.getChunks()) {
							chunks.add(chunk.getClientChunkName());
						}
						this.createChunks(chunks, restoredObject);
					}

					serverItem.setLatestVersion(restoredObject.getVersion());
					itemDao.put(serverItem);

					item.setChecksum(restoredObject.getChecksum());
					item.setChunks(chunks);
					item.setModifiedAt(restoredObject.getModifiedAt());
					item.setDeviceId(restoredObject.getDevice().getId());
					item.setFilename(restoredObject.getItem().getFilename());
					item.setSize(restoredObject.getSize());

					item.setIsFolder(serverItem.isFolder());
					item.setMimetype(serverItem.getMimetype());

					item.setParentVersion(serverItem.getClientParentFileVersion());

					item.setStatus(restoredObject.getStatus());
					item.setVersion(restoredObject.getVersion());

					APIRestoreMetadata response = new APIRestoreMetadata(item, true, 200, "");
					return response;
				} else {
					APIRestoreMetadata response = new APIRestoreMetadata(item, false, 400, "File not found.");
					return response;
				}
			} else {
				APIRestoreMetadata response = new APIRestoreMetadata(item, false, 400, "File not found.");
				return response;
			}
		} catch (DAOException e) {
			APIRestoreMetadata response = new APIRestoreMetadata(item, false, 400, e.getMessage());
			return response;
		}
	}

	@Override
	public APIDeleteResponse ApiDeleteMetadata(User user, ItemMetadata item) {
		List<ItemMetadata> filesToDelete;
		APIDeleteResponse response = null;

		try {

			filesToDelete = itemDao.getItemsById(item.getId());
			Workspace workspace = workspaceDAO.getByItemId(item.getId());
			if (filesToDelete.isEmpty()) {
				response = new APIDeleteResponse(null, false, 400, "File or folder does not exist.");
			} else {
				response = deleteItemsAPI(user, workspace, filesToDelete);
			}

		} catch (DAOException e) {
			response = new APIDeleteResponse(null, false, 400, "File or folder does not exist.");
		}

		return response;
	}

	@Override
	public APIGetVersions ApiGetVersions(User user, ItemMetadata item) {
		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (item.getId() == null) {
				// retrieve metadata from the root folder
				throw new DAOException(DAOError.FILE_NOT_FOUND);

			} else {

				// check if user has permission over this file
				List<User> users = userDao.findByItemId(item.getId());

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = itemDao.findItemVersionsById(item.getId());
			}

			success = true;

		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		}

		APIGetVersions response = new APIGetVersions(responseObject, success, errorCode, description);
		return response;
	}
	
	@Override
	public APIUserMetadata ApiGetUserMetadata(User user) {
		
		User userMetadata = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";
		
		try {
			
			// check if user has permission over this file
			userMetadata = userDao.findById(user.getId());

			if (userMetadata == null) {
				throw new DAOException(DAOError.USER_NOT_FOUND);
			}

			success = true;
		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		}
		
		APIUserMetadata response = new APIUserMetadata(userMetadata, success, errorCode, description);
		return response;
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

	private ItemMetadata getCurrentServerVersion(Item serverObject) throws DAOException {
		return getServerObjectVersion(serverObject, serverObject.getLatestVersion());
	}

	private ItemMetadata getServerObjectVersion(Item serverObject, long requestedVersion) throws DAOException {

		ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(serverObject.getId(), requestedVersion);

		return metadata;
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

	private void createChunks(List<String> chunksString, ItemVersion objectVersion) throws IllegalArgumentException,
			DAOException {

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

	private ItemMetadata saveNewItemAPI(User user, Workspace workspace, ItemMetadata itemToSave, ItemMetadata parent) {

		Long version = 1L;
		String fileName = itemToSave.getFilename();
		String mimetype = itemToSave.getMimetype();

		Long parentFileId = null;
		Long parentFileVersion = null;
		if (!parent.isRoot()) {
			parentFileId = parent.getId();
			parentFileVersion = parent.getVersion();
		}

		Long fileSize = itemToSave.getSize();
		Long checksum = itemToSave.getChecksum();
		String status = "NEW";
		Boolean folder = false;
		List<String> chunks = itemToSave.getChunks();

		Date date = new Date();

		// FIXME: return real path ?

		ItemMetadata object = new ItemMetadata(null, version, Constants.API_DEVICE_ID, parentFileId, parentFileVersion,
				status, date, checksum, fileSize, folder, fileName, mimetype, chunks);

		List<ItemMetadata> objects = new ArrayList<ItemMetadata>();
		objects.add(object);

		try {
			this.doCommit(user, workspace, apiDevice, objects);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return object;
	}

	private ItemMetadata saveNewVersionAPI(User user, ItemMetadata fileToSave, ItemMetadata fileToModify) {

		fileToModify.setStatus("CHANGED");
		fileToModify.setSize(fileToSave.getSize());
		fileToModify.setChunks(fileToSave.getChunks());
		fileToModify.setChecksum(fileToSave.getChecksum());
		fileToModify.setVersion(fileToModify.getVersion() + 1);

		Date date = new Date();
		fileToModify.setModifiedAt(date);

		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(fileToModify);

		try {
			this.doCommit(user, null, apiDevice, items);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileToModify;

	}

	private boolean createNewFolder(User user, ItemMetadata item, ItemMetadata parent) {

		// Create metadata

		if (!parent.isRoot()) {
			item.setParentId(parent.getId());
			item.setParentVersion(parent.getVersion());
		}

		item.setVersion(1L);
		item.setWorkspaceId(parent.getWorkspaceId());
		item.setStatus("NEW");
		item.setSize(0L);
		item.setIsFolder(true);
		item.setMimetype("inode/directory");
		item.setModifiedAt(new Date());
		item.setDeviceId(Constants.API_DEVICE_ID);
		item.setChecksum(0L);

		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(item);
		
		Workspace workspace = new Workspace(item.getWorkspaceId());

		try {
			List<CommitInfo> commitInfo = this.doCommit(user, workspace, apiDevice, items);
			return commitInfo.get(0).isCommitSucceed();
		} catch (DAOException e) {
			logger.error(e);
			return false;
		}
	}

	private APIDeleteResponse deleteItemsAPI(User user, Workspace workspace, List<ItemMetadata> filesToDelete) {

		List<ItemMetadata> items = new ArrayList<ItemMetadata>();

		for (ItemMetadata fileToDelete : filesToDelete) {

			if (fileToDelete.getStatus().equals("DELETED")) {
				continue;
			}

			fileToDelete.setStatus("DELETED");
			// fileToDelete.setFileSize(0L);
			fileToDelete.setChunks(new ArrayList<String>());
			// fileToDelete.setChecksum(0L);
			fileToDelete.setVersion(fileToDelete.getVersion() + 1);

			Date date = new Date();
			fileToDelete.setModifiedAt(date);

			items.add(fileToDelete);
		}

		Boolean success = false;
		ItemMetadata fileToDelete = null;

		try {
			List<CommitInfo> response = this.doCommit(user, workspace, apiDevice, items);

			if (!response.isEmpty()) {
				fileToDelete = response.get(0).getMetadata();
				success = true;
			}

		} catch (DAOException e) {
			logger.error(e);
		}

		APIDeleteResponse response = new APIDeleteResponse(fileToDelete, success, 0, "");
		return response;
	}

	private boolean userHasPermission(User user, List<User> users) {
		boolean hasPermission = false;
		for (User u : users) {
			if (u.getId().equals(user.getId())) {
				hasPermission = true;
				break;
			}
		}
		return hasPermission;
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

	@Override
	public Connection getConnection() {
		return this.connection;
	}
}
