package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

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
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.Item;
import com.stacksync.syncservice.model.ItemVersion;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.models.ItemMetadata;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.Commit;
import com.stacksync.syncservice.rpc.messages.GetWorkspaces;
import com.stacksync.syncservice.rpc.messages.GetWorkspacesResponse;
import com.stacksync.syncservice.util.Config;
import com.stacksync.syncservice.util.Constants;

public class SQLHandler implements Handler {
	private static final Logger logger = Logger.getLogger(SQLHandler.class
			.getName());
	private Connection connection;
	private WorkspaceDAO workspaceDAO;
	private UserDAO userDao;
	private DeviceDAO deviceDao;
	private ItemDAO itemDao;
	private ItemVersionDAO itemVersionDao;

	public SQLHandler(ConnectionPool pool) throws SQLException {
		connection = pool.getConnection();

		String dataSource = Config.getDatasource();

		DAOFactory factory = new DAOFactory(dataSource);

		workspaceDAO = factory.getWorkspaceDao(connection);
		userDao = factory.getUserDao(connection);
		deviceDao = factory.getDeviceDAO(connection);
		itemDao = factory.getItemDAO(connection);
		itemVersionDao = factory.getItemVersionDAO(connection);
	}

	@Override
	public CommitResult doCommit(Commit request) throws DAOException {

		HashMap<Long, Long> tempIds = new HashMap<Long, Long>();
		
		List<ItemMetadata> objects = request.getItems();
		Workspace workspace = workspaceDAO.findByName(request
				.getWorkspaceName());
		Device device = deviceDao.get(request.getDeviceId());
		List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();

		for (ItemMetadata object : objects) {
			
			ItemMetadata objectResponse = null;
			boolean committed;

			try {
				
				if(object.getParentId() != null){
					Long parentId = tempIds.get(object.getParentId());
					if (parentId != null){
						object.setParentId(parentId);
					}
				}
				
				this.commitObject(object, workspace, device);
				
				if(object.getTempId() != null)
				{
					tempIds.put(object.getTempId(), object.getId());
				}
				
				objectResponse = object;
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

			responseObjects.add(new CommitInfo(object.getVersion(), committed,
					objectResponse));
		}

		return new CommitResult(request.getRequestId(), responseObjects);
	}

	@Override
	public List<ItemMetadata> doGetChanges(String workspaceName, String user) {
		List<ItemMetadata> responseObjects = new ArrayList<ItemMetadata>();

		try {
			responseObjects = itemDao
					.getItemsByWorkspaceName(workspaceName);
		} catch (DAOException e) {
			logger.error(e.toString(), e);
		}

		return responseObjects;
	}

	@Override
	public APIGetMetadata ApiGetMetadata(String user, Long fileId,
			Boolean includeList, Boolean includeDeleted, Boolean includeChunks,
			Long version) {
		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (fileId == null) {
				// retrieve metadata from the root folder
				responseObject = this.itemDao.findByServerUserId(user,
						includeDeleted);

			} else {

				// check if user has permission over this file
				List<User> users = this.userDao.findByItemId(fileId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.itemDao.findById(fileId,
						includeList, version, includeDeleted, includeChunks);
			}

			success = true;

		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		}

		APIGetMetadata response = new APIGetMetadata(responseObject, success,
				errorCode, description);
		return response;
	}

	@Override
	public GetWorkspacesResponse doGetWorkspaces(GetWorkspaces workspacesRequest) {
		List<Workspace> workspaces = new ArrayList<Workspace>();
		Boolean succed = false;
		String description = "";

		try {
			String userCloudId = workspacesRequest.getUserCloudId();
			workspaces = workspaceDAO.findByUserCloudId(userCloudId);
			succed = true;
		} catch (DAOException e) {
			description = e.toString();
			logger.error(e.toString(), e);
		}

		String requestId = workspacesRequest.getRequestId();

		GetWorkspacesResponse workspacesResponse = new GetWorkspacesResponse(
				requestId, workspaces, succed, description);
		return workspacesResponse;
	}

	@Override
	public Long doUpdateDevice(Device device) {

		try {
			User user = userDao.findByCloudId(device.getUser().getCloudId());
			device.setUser(user);
		} catch (DAOException e1) {
			logger.error(e1);
			return -1L;
		}

		try {
			if (device.getId() == null) {
				deviceDao.add(device);
			} else {
				deviceDao.update(device);
			}
			return device.getId();
		} catch (DAOException e) {
			logger.error(e);
			return -1L;
		}
	}

	@Override
	public APICommitResponse ApiCommitMetadata(String userId,
			String workspaceName, Boolean overwrite, ItemMetadata fileToSave,
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
			object = saveNewObjecAPI(userId, workspaceName, fileToSave,
					parentMetadata);
		} else {
			if (overwrite) {
				object = saveNewVersionAPI(userId, workspaceName, fileToSave,
						fileToModify);
			} else {
				/*
				 * TODO Create conflict copy
				 */
			}
		}

		APICommitResponse responseAPI = new APICommitResponse(object, true, 0,
				"");
		return responseAPI;
	}

	@Override
	public APICreateFolderResponse ApiCreateFolder(String strUser,
			String workspace, ItemMetadata objectToSave,
			ItemMetadata parentMetadata) {
		String folderName = objectToSave.getFilename();
		List<ItemMetadata> files = parentMetadata.getChildren();

		ItemMetadata object = null;
		for (ItemMetadata file : files) {
			if (file.getFilename().equals(folderName)
					&& !file.getStatus().equals("DELETED")) {
				object = file;
				break;
			}
		}

		if (object == null) {
			object = this.createNewFolder(strUser, workspace, objectToSave,
					parentMetadata);

			APICreateFolderResponse responseAPI = new APICreateFolderResponse(
					object, true, 0, "");
			return responseAPI;
		} else {
			APICreateFolderResponse response = new APICreateFolderResponse(
					object, false, 400, "Folder already exists.");
			return response;
		}
	}

	@Override
	public APIRestoreMetadata ApiRestoreMetadata(String user, String workspace,
			ItemMetadata item) {
		try {

			Item serverItem = itemDao.findById(item.getId());
			ItemMetadata lastObjectVersion = itemDao.findById(
					item.getId(), false, null, false, false);
			if (serverItem != null && lastObjectVersion != null) {

				ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(
						serverItem.getId(), item.getVersion());

				ItemVersion restoredObject = new ItemVersion(metadata);

				if (restoredObject != null
						&& restoredObject.getStatus().compareTo(
								Status.DELETED.toString()) != 0) {
					restoredObject
							.setVersion(lastObjectVersion.getVersion() + 1);
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

					item.setParentVersion(serverItem
							.getClientParentFileVersion());

					item.setStatus(restoredObject.getStatus());
					item.setVersion(restoredObject.getVersion());

					APIRestoreMetadata response = new APIRestoreMetadata(item,
							true, 200, "");
					return response;
				} else {
					APIRestoreMetadata response = new APIRestoreMetadata(item,
							false, 400, "File not found.");
					return response;
				}
			} else {
				APIRestoreMetadata response = new APIRestoreMetadata(item,
						false, 400, "File not found.");
				return response;
			}
		} catch (DAOException e) {
			APIRestoreMetadata response = new APIRestoreMetadata(item, false,
					400, e.getMessage());
			return response;
		}
	}

	@Override
	public APIDeleteResponse ApiDeleteMetadata(String strUser,
			String workspace, ItemMetadata object) {
		List<ItemMetadata> filesToDelete;
		APIDeleteResponse response = null;

		try {

			filesToDelete = itemDao.getItemsById(object.getId());

			if (filesToDelete.isEmpty()) {
				response = new APIDeleteResponse(null, false, 400,
						"File or folder does not exist.");
			} else {
				response = deleteObjectsAPI(strUser, workspace, filesToDelete);
			}

		} catch (DAOException e) {
			response = new APIDeleteResponse(null, false, 400,
					"File or folder does not exist.");
		}

		return response;
	}

	@Override
	public APIGetVersions ApiGetVersions(String user, Long itemId) {
		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (itemId == null) {
				// retrieve metadata from the root folder
				throw new DAOException(DAOError.FILE_NOT_FOUND);

			} else {

				// check if user has permission over this file
				List<User> users = userDao.findByItemId(itemId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = itemDao.findItemVersionsById(itemId);
			}

			success = true;

		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		}

		APIGetVersions response = new APIGetVersions(responseObject, success,
				errorCode, description);
		return response;
	}

	/*
	 * Private functions
	 */

	private void commitObject(ItemMetadata item, Workspace workspace,
			Device device) throws CommitWrongVersionNoParent,
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

	private void saveNewObject(ItemMetadata metadata, Workspace workspace,
			Device device) throws DAOException {
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

	private ItemMetadata getCurrentServerVersion(Item serverObject)
			throws DAOException {
		return getServerObjectVersion(serverObject,
				serverObject.getLatestVersion());
	}

	private ItemMetadata getServerObjectVersion(Item serverObject,
			long requestedVersion) throws DAOException {

		ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(
				serverObject.getId(), requestedVersion);

		return metadata;
	}

	private void saveNewVersion(ItemMetadata metadata, Item serverItem,
			Workspace workspace, Device device) throws DAOException {

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
			if (status.equals(Status.RENAMED.toString())
					|| status.equals(Status.MOVED.toString())) {

				serverItem.setFilename(metadata.getFilename());

				Long parentFileId = metadata.getParentId();
				if (parentFileId == null) {
					serverItem.setClientParentFileVersion(null);
				} else {
					serverItem.setClientParentFileVersion(metadata
							.getParentVersion());
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

	private void saveExistentVersion(Item serverObject,
			ItemMetadata clientMetadata) throws CommitWrongVersion,
			CommitExistantVersion, DAOException {

		ItemMetadata serverMetadata = this.getServerObjectVersion(serverObject,
				clientMetadata.getVersion());

		if (!clientMetadata.equals(serverMetadata)) {
			throw new CommitWrongVersion("Invalid version.", serverObject);
		}

		boolean lastVersion = (serverObject.getLatestVersion()
				.equals(clientMetadata.getVersion()));

		if (!lastVersion) {
			throw new CommitExistantVersion("This version already exists.",
					serverObject, clientMetadata.getVersion());
		}

	}

	private void createChunks(List<String> chunksString,
			ItemVersion objectVersion) throws IllegalArgumentException,
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

	private ItemMetadata saveNewObjecAPI(String userId, String workspaceName,
			ItemMetadata objectToSave, ItemMetadata parent) {

		Random rand = new Random();

		// Create metadata
		Long itemId = rand.nextLong();
		Long version = 1L;
		String fileName = objectToSave.getFilename();
		String mimetype = objectToSave.getMimetype();

		Long parentFileId = null;
		Long parentFileVersion = null;
		if (!parent.isRoot()) {
			parentFileId = parent.getId();
			parentFileVersion = parent.getVersion();
		}

		Long fileSize = objectToSave.getSize();
		Long checksum = objectToSave.getChecksum();
		String status = "NEW";
		Boolean folder = false;
		List<String> chunks = objectToSave.getChunks();

		Date date = new Date();

		// FIXME: return real path ?

		ItemMetadata object = new ItemMetadata(itemId, version,
				Constants.API_DEVICE_ID, parentFileId, parentFileVersion,
				status, date, checksum, fileSize, folder, fileName, mimetype,
				"", chunks);

		List<ItemMetadata> objects = new ArrayList<ItemMetadata>();
		objects.add(object);

		Commit message = new Commit(userId, "api-" + rand.nextInt(), objects,
				Constants.API_DEVICE_ID, workspaceName);

		try {
			this.doCommit(message);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return object;
	}

	private ItemMetadata saveNewVersionAPI(String userId, String workspaceName,
			ItemMetadata fileToSave, ItemMetadata fileToModify) {

		Random rand = new Random();

		fileToModify.setStatus("CHANGED");
		fileToModify.setSize(fileToSave.getSize());
		fileToModify.setChunks(fileToSave.getChunks());
		fileToModify.setChecksum(fileToSave.getChecksum());
		fileToModify.setVersion(fileToModify.getVersion() + 1);

		Date date = new Date();
		fileToModify.setModifiedAt(date);

		List<ItemMetadata> objects = new ArrayList<ItemMetadata>();
		objects.add(fileToModify);

		Commit message = new Commit(userId, "api-" + rand.nextInt(), objects,
				Constants.API_DEVICE_ID, workspaceName);

		try {
			this.doCommit(message);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileToModify;

	}

	private ItemMetadata createNewFolder(String strUser, String workspace,
			ItemMetadata objectToSave, ItemMetadata parent) {

		Random rand = new Random();

		// Create metadata
		Long itemId = rand.nextLong();
		Long version = 1L;

		Long parentFileID = null;
		Long parentFileVersion = null;
		if (!parent.isRoot()) {
			parentFileID = parent.getId();
			parentFileVersion = parent.getVersion();
		}

		String status = "NEW";
		Long fileSize = 0L;
		List<String> chunks = null;
		Long checksum = 0L;
		Boolean folder = true;

		Date date = new Date();

		// FIXME: return real path ?

		ItemMetadata object = new ItemMetadata(itemId, version,
				Constants.API_DEVICE_ID, parentFileID, parentFileVersion,
				status, date, checksum, fileSize, folder, objectToSave.getFilename(), "inode/directory",
				"", chunks);

		List<ItemMetadata> objects = new ArrayList<ItemMetadata>();
		objects.add(object);

		Commit message = new Commit(strUser, "api-" + rand.nextInt(), objects,
				Constants.API_DEVICE_ID, workspace);

		try {
			this.doCommit(message);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return object;
	}

	private APIDeleteResponse deleteObjectsAPI(String userId,
			String workspaceName, List<ItemMetadata> filesToDelete) {
		Random rand = new Random();
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

		Commit message = new Commit(userId, "api-" + rand.nextInt(), items,
				Constants.API_DEVICE_ID, workspaceName);

		Boolean success = false;
		ItemMetadata fileToDelete = null;

		try {
			CommitResult response = this.doCommit(message);

			if (!response.getObjects().isEmpty()) {
				fileToDelete = response.getObjects().get(0).getMetadata();
				success = true;
			}

		} catch (DAOException e) {
			logger.error(e);
		}

		APIDeleteResponse response = new APIDeleteResponse(fileToDelete,
				success, 0, "");
		return response;
	}

	private boolean userHasPermission(String userCloudId, List<User> users) {
		boolean hasPermission = false;
		for (User user : users) {
			if (user.getCloudId().equals(userCloudId)) {
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
