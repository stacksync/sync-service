package com.stacksync.syncservice.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.Object1DAO;
import com.stacksync.syncservice.db.ObjectVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.model.ObjectVersion;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.CommitInfo;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.models.ObjectMetadata;
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

public class SQLHandler implements Handler {
	private static final Logger logger = Logger.getLogger(SQLHandler.class
			.getName());
	private Connection connection;
	private WorkspaceDAO workspaceDAO;
	private UserDAO userDao;
	private DeviceDAO deviceDao;
	private Object1DAO objectDao;
	private ObjectVersionDAO oversionDao;

	public SQLHandler(ConnectionPool pool) throws SQLException {
		connection = pool.getConnection();

		String dataSource = Config.getDatasource();

		DAOFactory factory = new DAOFactory(dataSource);

		workspaceDAO = factory.getWorkspaceDao(connection);
		userDao = factory.getUserDao(connection);
		deviceDao = factory.getDeviceDAO(connection);
		objectDao = factory.getObject1DAO(connection);
		oversionDao = factory.getObjectVersionDAO(connection);
	}

	@Override
	public CommitResult doCommit(Commit request) throws DAOException {

		List<ObjectMetadata> objects = request.getObjects();
		Workspace workspace = workspaceDAO.findByName(request
				.getWorkspaceName());
		Device device = this.getOrCreateDevice(request.getDeviceName(),
				request.getUser());
		List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();

		for (ObjectMetadata object : objects) {

			ObjectMetadata objectResponse = null;
			boolean committed;

			try {
				this.commitObject(object, workspace, device);
				objectResponse = object;
				committed = true;
			} catch (CommitWrongVersion e) {
				Object1 serverObject = e.getObject();
				objectResponse = this.getCurrentServerVersion(serverObject);
				committed = false;
			} catch (CommitWrongVersionNoParent e) {
				committed = false;
			} catch (CommitExistantVersion e) {
				Object1 serverObject = e.getObject();
				objectResponse = this.getCurrentServerVersion(serverObject);
				committed = true;
			}

			responseObjects.add(new CommitInfo(object.getFileId(), object
					.getRootId(), object.getVersion(), committed,
					objectResponse));
		}

		return new CommitResult(request.getRequestId(), responseObjects);
	}

	@Override
	public List<ObjectMetadata> doGetChanges(String workspaceName, String user) {
		List<ObjectMetadata> responseObjects = new ArrayList<ObjectMetadata>();

		try {
			responseObjects = objectDao
					.getObjectMetadataByWorkspaceName(workspaceName);
		} catch (DAOException e) {
			logger.error(e.toString(), e);
		}

		return responseObjects;
	}

	@Override
	public APIGetMetadata ApiGetMetadata(String user, Long fileId,
			Boolean includeList, Boolean includeDeleted, Boolean includeChunks,
			Long version) {
		ObjectMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (fileId == null) {
				// retrieve metadata from the root folder
				responseObject = this.objectDao.findByServerUserId(user,
						includeDeleted);

			} else {

				// check if user has permission over this file
				List<User> users = this.userDao.findByClientFileId(fileId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.objectDao.findByClientFileId(fileId,
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
			String workspaceName, Boolean overwrite, ObjectMetadata fileToSave,
			ObjectMetadata parentMetadata) {
		List<ObjectMetadata> files = parentMetadata.getContent();

		ObjectMetadata fileToModify = null;
		for (ObjectMetadata file : files) {
			if (file.getFileName().equals(fileToSave.getFileName())) {
				fileToModify = file;
				break;
			}
		}

		ObjectMetadata object = null;
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
			String workspace, ObjectMetadata objectToSave,
			ObjectMetadata parentMetadata) {
		String folderName = objectToSave.getFileName();
		List<ObjectMetadata> files = parentMetadata.getContent();

		ObjectMetadata object = null;
		for (ObjectMetadata file : files) {
			if (file.getFileName().equals(folderName)
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
			ObjectMetadata object) {
		try {

			Object1 objDb = objectDao.findByClientId(object.getFileId());
			ObjectMetadata lastObjectVersion = objectDao.findByClientFileId(
					object.getFileId(), false, null, false, false);
			if (objDb != null && lastObjectVersion != null) {

				ObjectVersion restoredObject = oversionDao
						.findByObjectIdAndVersion(objDb.getId(),
								object.getVersion());

				if (restoredObject != null
						&& restoredObject.getClientStatus().compareTo(
								Status.DELETED.toString()) != 0) {
					restoredObject
							.setVersion(lastObjectVersion.getVersion() + 1);
					restoredObject.setClientStatus(Status.CHANGED.toString());

					// save restoredObject
					oversionDao.add(restoredObject);

					List<String> chunks = new ArrayList<String>();
					// If no folder, create new chunks
					if (!restoredObject.getChunks().isEmpty()) {
						for (Chunk chunk : restoredObject.getChunks()) {
							chunks.add(chunk.getClientChunkName());
						}
						this.createChunks(chunks, restoredObject);
					}

					objDb.setLatestVersion(restoredObject.getVersion());
					objectDao.put(objDb);

					object.setChecksum(restoredObject.getChecksum());
					object.setChunks(chunks);
					object.setClientDateModified(restoredObject
							.getClientDateModified());
					object.setClientName(restoredObject.getClientName());
					object.setFileName(restoredObject.getClientName());
					object.setFilePath(restoredObject.getClientFilePath());
					object.setFileSize(restoredObject.getClientFileSize());

					object.setFolder(objDb.getClientFolder());
					object.setMimetype(objDb.getClientFileMimetype());

					object.setParentFileId(objDb.getClientParentFileId());
					object.setParentFileVersion(objDb
							.getClientParentFileVersion());

					object.setRootId(objDb.getRootId());
					object.setServerDateModified(restoredObject
							.getServerDateModified());
					object.setStatus(restoredObject.getClientStatus());
					object.setVersion(restoredObject.getVersion());

					APIRestoreMetadata response = new APIRestoreMetadata(
							object, true, 200, "");
					return response;
				} else {
					APIRestoreMetadata response = new APIRestoreMetadata(
							object, false, 400, "File not found.");
					return response;
				}
			} else {
				APIRestoreMetadata response = new APIRestoreMetadata(object,
						false, 400, "File not found.");
				return response;
			}
		} catch (DAOException e) {
			APIRestoreMetadata response = new APIRestoreMetadata(object, false,
					400, e.getMessage());
			return response;
		}
	}

	@Override
	public APIDeleteResponse ApiDeleteMetadata(String strUser,
			String workspace, ObjectMetadata object) {
		List<ObjectMetadata> filesToDelete;
		APIDeleteResponse response = null;

		try {

			filesToDelete = objectDao.getObjectsByClientFileId(object
					.getFileId());

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
	public APIGetVersions ApiGetVersions(String user, Long fileId) {
		ObjectMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (fileId == null) {
				// retrieve metadata from the root folder
				throw new DAOException(DAOError.FILE_NOT_FOUND);

			} else {

				// check if user has permission over this file
				List<User> users = userDao.findByClientFileId(fileId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = objectDao
						.findObjectVersionsByClientFileId(fileId);
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
	private Device getOrCreateDevice(String deviceName, String clientCloudId) {
		Device device = null;
		try {
			device = deviceDao.findByName(deviceName);
			if (device == null) {
				User user = userDao.findByCloudId(clientCloudId);
				device = new Device();
				device.setName(deviceName);
				device.setUser(user);
				deviceDao.add(device);
			}
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return device;
	}

	private void commitObject(ObjectMetadata object, Workspace workspace,
			Device device) throws CommitWrongVersionNoParent,
			CommitWrongVersion, CommitExistantVersion, DAOException {

		long fileID = object.getFileId();
		Object1 serverObject = objectDao.findByClientFileIdAndWorkspace(fileID,
				workspace.getId());

		// Check if this object already exists in the server.
		if (serverObject == null) {
			if (object.getVersion() == 1) {
				this.saveNewObject(object, workspace, device);
			} else {
				throw new CommitWrongVersionNoParent();
			}
			return;
		}

		// Check if the client version already exists in the server
		long serverVersion = serverObject.getLatestVersion();
		long clientVersion = object.getVersion();
		boolean existVersionInServer = (serverVersion >= clientVersion);

		if (existVersionInServer) {
			this.saveExistentVersion(serverObject, object);
		} else {
			// Check if version is correct
			if (serverVersion + 1 == clientVersion) {
				this.saveNewVersion(object, serverObject, workspace, device);
			} else {
				throw new CommitWrongVersion("Invalid version.", serverObject);
			}
		}
	}

	private void saveNewObject(ObjectMetadata metadata, Workspace workspace,
			Device device) throws DAOException {
		// Create workspace and parent instances
		Long parentFileId = metadata.getParentFileId();
		Object1 parent = null;
		if (parentFileId != null) {
			parent = objectDao.findByClientFileIdAndWorkspace(parentFileId,
					workspace.getId());
		}

		beginTransaction();

		try {
			// Insert object to DB
			Object1 object = new Object1();
			object.setRootId(metadata.getRootId());
			object.setClientFileId(metadata.getFileId());
			object.setClientFileName(metadata.getFileName());
			object.setClientFileMimetype(metadata.getMimetype());
			object.setClientFolder(metadata.isFolder());
			object.setClientParentRootId(metadata.getParentRootId());
			object.setClientParentFileId(metadata.getParentFileId());
			object.setClientParentFileVersion(metadata.getParentFileVersion());

			object.setLatestVersion(metadata.getVersion());
			object.setWorkspace(workspace);
			object.setParent(parent);

			objectDao.put(object);

			// Insert objectVersion
			ObjectVersion objectVersion = new ObjectVersion();
			objectVersion.setVersion(metadata.getVersion());
			objectVersion.setServerDateModified(metadata
					.getServerDateModified());
			objectVersion.setChecksum(metadata.getChecksum());
			objectVersion.setClientDateModified(metadata
					.getClientDateModified());
			objectVersion.setClientStatus(metadata.getStatus());
			objectVersion.setClientFileSize(metadata.getFileSize());
			objectVersion.setClientName(metadata.getClientName());
			objectVersion.setClientFilePath(metadata.getFilePath());

			objectVersion.setObject(object);
			objectVersion.setDevice(device);
			oversionDao.add(objectVersion);

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

	private ObjectMetadata getCurrentServerVersion(Object1 serverObject)
			throws DAOException {
		return getServerObjectVersion(serverObject,
				serverObject.getLatestVersion());
	}

	private ObjectMetadata getServerObjectVersion(Object1 serverObject,
			long requestedVersion) throws DAOException {
		ObjectVersion version = oversionDao.findByObjectIdAndVersion(
				serverObject.getId(), requestedVersion);

		String rootId = serverObject.getRootId();
		Long fileId = serverObject.getClientFileId();
		long versionNumber = requestedVersion;
		String parentRootId = serverObject.getClientParentRootId();

		Long parentFileId = null;
		if (serverObject.getClientParentFileId() != null) {
			parentFileId = serverObject.getClientParentFileId();
		}

		Long parentFileVersion = null;
		if (serverObject.getClientParentFileVersion() != null) {
			parentFileVersion = serverObject.getClientParentFileVersion();
		}

		Date serverDateModified = version.getServerDateModified();
		String status = version.getClientStatus();
		Date clientDateModified = version.getClientDateModified();
		long checksum = version.getChecksum();
		String clientName = version.getClientName();
		long fileSize = version.getClientFileSize();
		boolean folder = serverObject.getClientFolder();
		String fileName = serverObject.getClientFileName();
		String path = version.getClientFilePath();
		String mimetype = serverObject.getClientFileMimetype();

		Collection<Chunk> chunks = oversionDao.findChunks(version.getId());
		List<String> chunksName = new ArrayList<String>();
		for (Chunk c : chunks) {
			chunksName.add(c.getClientChunkName());
		}

		ObjectMetadata metadata = new ObjectMetadata(rootId, fileId,
				versionNumber, parentRootId, parentFileId, parentFileVersion,
				serverDateModified, status, clientDateModified, checksum,
				clientName, chunksName, fileSize, folder, fileName, path,
				mimetype);

		return metadata;
	}

	private void saveNewVersion(ObjectMetadata metadata, Object1 serverObject,
			Workspace workspace, Device device) throws DAOException {

		beginTransaction();

		try {
			// Create new objectVersion
			ObjectVersion objectVersion = new ObjectVersion();
			objectVersion.setVersion(metadata.getVersion());
			objectVersion.setServerDateModified(metadata
					.getServerDateModified());
			objectVersion.setChecksum(metadata.getChecksum());
			objectVersion.setClientDateModified(metadata
					.getClientDateModified());
			objectVersion.setClientStatus(metadata.getStatus());
			objectVersion.setClientFileSize(metadata.getFileSize());
			objectVersion.setClientName(metadata.getClientName());
			objectVersion.setClientFilePath(metadata.getFilePath());

			objectVersion.setObject(serverObject);
			objectVersion.setDevice(device);
			objectVersion.setServerDateModified(new Date());

			oversionDao.add(objectVersion);

			// If no folder, create new chunks
			if (!metadata.isFolder()) {
				List<String> chunks = metadata.getChunks();
				this.createChunks(chunks, objectVersion);
			}

			// TODO To Test!!
			String status = metadata.getStatus();
			if (status.equals(Status.RENAMED.toString())
					|| status.equals(Status.MOVED.toString())) {

				serverObject.setClientFileName(metadata.getFileName());

				Long parentFileId = metadata.getParentFileId();
				if (parentFileId == null) {
					serverObject.setClientParentFileId(null);
					serverObject.setClientParentFileVersion(null);
					serverObject.setClientParentRootId(null);
				} else {
					serverObject.setClientParentFileId(parentFileId);
					serverObject.setClientParentRootId(metadata
							.getParentRootId());
					serverObject.setClientParentFileVersion(metadata
							.getParentFileVersion());
					Object1 parent = objectDao.findByClientFileIdAndWorkspace(
							parentFileId, workspace.getId());
					serverObject.setParent(parent);
				}

			}

			// Update object latest version
			serverObject.setLatestVersion(metadata.getVersion());
			objectDao.put(serverObject);

			commitTransaction();
		} catch (Exception e) {
			logger.error(e);
			rollbackTransaction();
		}
	}

	private void saveExistentVersion(Object1 serverObject,
			ObjectMetadata clientMetadata) throws CommitWrongVersion,
			CommitExistantVersion, DAOException {

		ObjectMetadata serverMetadata = this.getServerObjectVersion(
				serverObject, clientMetadata.getVersion());

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
			ObjectVersion objectVersion) throws IllegalArgumentException,
			DAOException {

		if (chunksString.size() > 0) {
			List<Chunk> chunks = new ArrayList<Chunk>();
			int i = 0;

			for (String chunkName : chunksString) {
				chunks.add(new Chunk(chunkName, i));
				i++;
			}

			oversionDao.instertChunks(chunks, objectVersion.getId());
		}
	}

	private ObjectMetadata saveNewObjecAPI(String userId, String workspaceName,
			ObjectMetadata objectToSave, ObjectMetadata parent) {

		Random rand = new Random();

		// Create metadata
		Long fileID = rand.nextLong();
		String rootID = "stacksync";
		Long version = 1L;
		String fileName = objectToSave.getFileName();
		String mimetype = objectToSave.getMimetype();

		Long parentFileID = null;
		Long parentFileVersion = null;
		String parentRootID = null;
		if (!parent.isRoot()) {
			parentFileID = parent.getFileId();
			parentFileVersion = parent.getVersion();
			parentRootID = parent.getRootId();
		}

		Long fileSize = objectToSave.getFileSize();
		Long checksum = objectToSave.getChecksum();
		String status = "NEW";
		Boolean folder = false;
		List<String> chunks = objectToSave.getChunks();

		Date date = new Date();

		String path;
		if (parent.isRoot()) {
			path = "/";
		} else {
			path = parent.getFilePath() + "/" + parent.getFileName();
			path = path.replace("//", "/");
		}

		ObjectMetadata object = new ObjectMetadata(rootID, fileID, version,
				parentRootID, parentFileID, parentFileVersion, date, status,
				date, checksum, "web", chunks, fileSize, folder, fileName,
				path, mimetype);

		List<ObjectMetadata> objects = new ArrayList<ObjectMetadata>();
		objects.add(object);

		Commit message = new Commit(userId, "web-" + rand.nextInt(), objects,
				"web", workspaceName);

		try {
			this.doCommit(message);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return object;
	}

	private ObjectMetadata saveNewVersionAPI(String userId,
			String workspaceName, ObjectMetadata fileToSave,
			ObjectMetadata fileToModify) {

		Random rand = new Random();

		fileToModify.setStatus("CHANGED");
		fileToModify.setFileSize(fileToSave.getFileSize());
		fileToModify.setChunks(fileToSave.getChunks());
		fileToModify.setChecksum(fileToSave.getChecksum());
		fileToModify.setClientName("web");
		fileToModify.setVersion(fileToModify.getVersion() + 1);

		String path = fileToModify.getFilePath();
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
			fileToModify.setFilePath(path);
		}

		Date date = new Date();
		fileToModify.setServerDateModified(date);
		fileToModify.setClientDateModified(date);

		List<ObjectMetadata> objects = new ArrayList<ObjectMetadata>();
		objects.add(fileToModify);

		Commit message = new Commit(userId, "web-" + rand.nextInt(), objects,
				"web", workspaceName);

		try {
			this.doCommit(message);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileToModify;

	}

	private ObjectMetadata createNewFolder(String strUser, String workspace,
			ObjectMetadata objectToSave, ObjectMetadata parent) {

		Random rand = new Random();

		// Create metadata
		Long fileID = rand.nextLong();
		String rootID = "stacksync";
		Long version = 1L;

		Long parentFileID = null;
		Long parentFileVersion = null;
		String parentRootID = null;
		if (!parent.isRoot()) {
			parentFileID = parent.getFileId();
			parentFileVersion = parent.getVersion();
			parentRootID = parent.getRootId();
		}

		String status = "NEW";
		Long fileSize = 0L;
		List<String> chunks = null;
		Long checksum = 0L;
		Boolean folder = true;

		Date date = new Date();

		String path;
		if (parent.isRoot()) {
			path = "/";
		} else {
			path = parent.getFilePath() + "/" + parent.getFileName();
			path = path.replace("//", "/");
		}

		ObjectMetadata object = new ObjectMetadata(rootID, fileID, version,
				parentRootID, parentFileID, parentFileVersion, date, status,
				date, checksum, "web", chunks, fileSize, folder,
				objectToSave.getFileName(), path, "inode/directory");

		List<ObjectMetadata> objects = new ArrayList<ObjectMetadata>();
		objects.add(object);

		Commit message = new Commit(strUser, "web-" + rand.nextInt(), objects,
				"web", workspace);

		try {
			this.doCommit(message);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return object;
	}

	private APIDeleteResponse deleteObjectsAPI(String userId,
			String workspaceName, List<ObjectMetadata> filesToDelete) {
		Random rand = new Random();
		List<ObjectMetadata> objects = new ArrayList<ObjectMetadata>();

		for (ObjectMetadata fileToDelete : filesToDelete) {

			if (fileToDelete.getStatus().equals("DELETED")) {
				continue;
			}

			fileToDelete.setStatus("DELETED");
			// fileToDelete.setFileSize(0L);
			fileToDelete.setChunks(new ArrayList<String>());
			// fileToDelete.setChecksum(0L);
			fileToDelete.setClientName("web");
			fileToDelete.setVersion(fileToDelete.getVersion() + 1);

			String path = fileToDelete.getFilePath();
			if (path.length() > 1 && path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
				fileToDelete.setFilePath(path);
			}

			Date date = new Date();
			fileToDelete.setServerDateModified(date);
			fileToDelete.setClientDateModified(date);

			objects.add(fileToDelete);
		}

		Commit message = new Commit(userId, "web-" + rand.nextInt(), objects,
				"web", workspaceName);

		Boolean success = false;
		ObjectMetadata fileToDelete = null;

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
