package com.stacksync.syncservice.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIGetWorkspaceInfoResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.util.Constants;

public class SQLAPIHandler extends Handler implements APIHandler {

	private static final Logger logger = Logger.getLogger(SQLAPIHandler.class.getName());

	private Device apiDevice = new Device(Constants.API_DEVICE_ID);

	public SQLAPIHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
		super(pool);
	}

	@Override
	public APIGetMetadata getMetadata(User user, Long fileId, Boolean includeChunks, Long version, Boolean isFolder) {

		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (fileId == null) {
				// retrieve metadata from the root folder
				responseObject = this.itemDao.findByUserId(user.getId(), false);
			} else {

				// check if user has permission on this file
				List<User> users = this.userDao.findByItemId(fileId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.itemDao.findById(fileId, false, version, false, includeChunks);
			}

			if (responseObject.isFolder() != isFolder) {
				throw new DAOException(DAOError.FILE_NOT_FOUND);
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

	public APIGetMetadata getFolderContent(User user, Long folderId, Boolean includeDeleted) {

		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (folderId == null) {
				// retrieve metadata from the root folder
				responseObject = this.itemDao.findByUserId(user.getId(), includeDeleted);
			} else {

				// check if user has permission on this file
				List<User> users = this.userDao.findByItemId(folderId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.itemDao.findById(folderId, true, null, includeDeleted, false);
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
	public APICommitResponse createFile(User user, ItemMetadata fileToSave) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			return new APICommitResponse(fileToSave, false, 404, "User not found.");
		}

		// Get user workspaces
		try {
			List<Workspace> workspaces = workspaceDAO.getByUserId(user.getId());
			user.setWorkspaces(workspaces);
		} catch (DAOException e) {
			logger.error(e);
			return new APICommitResponse(fileToSave, false, 404, "No workspaces found for the user.");
		}

		boolean includeList = true;
		Long version = null;
		boolean includeDeleted = false;
		boolean includeChunks = false;

		// check that the given parent ID exists
		ItemMetadata parent;
		if (fileToSave.getParentId() != null) {
			try {
				parent = itemDao
						.findById(fileToSave.getParentId(), includeList, version, includeDeleted, includeChunks);
				fileToSave.setParentVersion(parent.getVersion());

				// check if parent is a folder
				if (!parent.isFolder()) {
					return new APICommitResponse(fileToSave, false, 400, "Parent must be a folder, not a file.");
				}

			} catch (DAOException e) {
				return new APICommitResponse(fileToSave, false, 404, "Parent folder not found");
			}
		} else {
			try {
				parent = this.itemDao.findByUserId(user.getId(), includeDeleted);
				Workspace parentWorkspace = workspaceDAO.getDefaultWorkspaceByUserId(user.getId());
				parent.setWorkspaceId(parentWorkspace.getId());
			} catch (DAOException e) {
				return new APICommitResponse(fileToSave, false, e.getError().getCode(), e.getMessage());
			}
		}

		// check if the user has permission on the file and parent
		boolean permissionParent = false;
		for (Workspace w : user.getWorkspaces()) {
			if (parent.isRoot() || w.getId().equals(parent.getWorkspaceId())) {
				permissionParent = true;
				break;
			}
		}
		if (!permissionParent) {
			return new APICommitResponse(fileToSave, false, 403, "You are not allowed to modify this file");
		}

		// check if there is already a file with the same name
		boolean repeated = false;
		for (ItemMetadata child : parent.getChildren()) {
			if (child.getFilename().equals(fileToSave.getFilename())) {
				repeated = true;
				break;
			}
		}
		if (repeated) {
			return new APICommitResponse(fileToSave, false, 400,
					"This name is already used in the same folder. Please use a different one. ");
		}

		APICommitResponse responseAPI;

		try {
			saveNewItemAPI(user, fileToSave, parent);
			responseAPI = new APICommitResponse(fileToSave, true, 0, "");

		} catch (Exception e) {
			logger.error(e);
			responseAPI = new APICommitResponse(fileToSave, false, 500, e.toString());
		}

		return responseAPI;
	}

	@Override
	public APICommitResponse updateData(User user, ItemMetadata fileToUpdate) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			return new APICommitResponse(fileToUpdate, false, 404, "User not found.");
		}

		// Get user workspaces
		try {
			List<Workspace> workspaces = workspaceDAO.getByUserId(user.getId());
			user.setWorkspaces(workspaces);
		} catch (DAOException e) {
			logger.error(e);
			return new APICommitResponse(fileToUpdate, false, 404, "No workspaces found for the user.");
		}

		boolean includeList = true;
		Long version = null;
		boolean includeDeleted = false;
		boolean includeChunks = false;

		// check that the given file ID exists
		ItemMetadata file;
		try {
			file = itemDao.findById(fileToUpdate.getId(), includeList, version, includeDeleted, includeChunks);
		} catch (DAOException e) {
			return new APICommitResponse(fileToUpdate, false, 404, "File not found");
		}

		// check if the user has permission on the file and parent
		boolean permission = false;
		for (Workspace w : user.getWorkspaces()) {
			if (w.getId().equals(file.getWorkspaceId())) {
				permission = true;
			}
		}
		if (!permission) {
			return new APICommitResponse(fileToUpdate, false, 403, "You are not allowed to modify this file");
		}

		// update file attributes

		file.setMimetype(fileToUpdate.getMimetype());
		file.setChecksum(fileToUpdate.getChecksum());
		file.setSize(fileToUpdate.getSize());
		file.setChunks(fileToUpdate.getChunks());
		file.setVersion(file.getVersion() + 1L);
		file.setStatus(Status.CHANGED.toString());

		// Commit the file
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(file);

		Workspace workspace = new Workspace(file.getWorkspaceId());

		try {
			this.doCommit(user, workspace, apiDevice, items);
		} catch (DAOException e) {
			return new APICommitResponse(fileToUpdate, false, e.getError().getCode(), e.getMessage());
		}

		APICommitResponse responseAPI = new APICommitResponse(file, true, 0, "");
		return responseAPI;
	}

	@Override
	public APICommitResponse updateMetadata(User user, ItemMetadata fileToUpdate) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			return new APICommitResponse(fileToUpdate, false, 404, "User not found.");
		}

		// Get user workspaces
		try {
			List<Workspace> workspaces = workspaceDAO.getByUserId(user.getId());
			user.setWorkspaces(workspaces);
		} catch (DAOException e) {
			logger.error(e);
			return new APICommitResponse(fileToUpdate, false, 404, "No workspaces found for the user.");
		}

		boolean includeList = true;
		Long version = null;
		boolean includeDeleted = false;
		boolean includeChunks = true;

		// check that the given file ID exists
		ItemMetadata file;
		try {
			file = itemDao.findById(fileToUpdate.getId(), includeList, version, includeDeleted, includeChunks);
		} catch (DAOException e) {
			return new APICommitResponse(fileToUpdate, false, 404, "File not found");
		}

		// check that the given parent ID exists
		ItemMetadata parent;
		if (fileToUpdate.getParentId() != null) {
			try {
				parent = itemDao.findById(fileToUpdate.getParentId(), includeList, version, includeDeleted,
						includeChunks);

				// check if parent is a folder
				if (!parent.isFolder()) {
					return new APICommitResponse(fileToUpdate, false, 400, "Parent must be a folder, not a file.");
				}

			} catch (DAOException e) {
				return new APICommitResponse(fileToUpdate, false, 404, "Parent folder not found");
			}
		} else {
			try {
				parent = this.itemDao.findByUserId(user.getId(), includeDeleted);
			} catch (DAOException e) {
				return new APICommitResponse(fileToUpdate, false, e.getError().getCode(), e.getMessage());
			}
		}

		// check if the user has permission on the file and parent
		boolean permissionFile = false;
		boolean permissionParent = false;
		for (Workspace w : user.getWorkspaces()) {
			if (w.getId().equals(file.getWorkspaceId())) {
				permissionFile = true;
			}
			if (parent.isRoot() || w.getId().equals(parent.getWorkspaceId())) {
				permissionParent = true;
			}
		}
		if (!permissionFile || !permissionParent) {
			return new APICommitResponse(fileToUpdate, false, 403, "You are not allowed to modify this file");
		}

		// check if there is already a file with the same name
		boolean repeated = false;
		for (ItemMetadata child : parent.getChildren()) {
			if (child.getFilename().equals(fileToUpdate.getFilename())) {
				repeated = true;
			}
		}
		if (repeated) {
			return new APICommitResponse(fileToUpdate, false, 400,
					"This name is already used in the same folder. Please use a different one. ");
		}

		// update file attributes
		file.setFilename(fileToUpdate.getFilename());
		file.setParentId(fileToUpdate.getParentId());
		file.setVersion(file.getVersion() + 1L);
		file.setStatus(Status.RENAMED.toString());

		// Commit the file
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(file);

		Workspace workspace = new Workspace(file.getWorkspaceId());

		try {
			this.doCommit(user, workspace, apiDevice, items);
		} catch (DAOException e) {
			return new APICommitResponse(fileToUpdate, false, e.getError().getCode(), e.getMessage());
		}

		APICommitResponse responseAPI = new APICommitResponse(file, true, 0, "");
		return responseAPI;
	}

	@Override
	public APICreateFolderResponse createFolder(User user, ItemMetadata item) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			APICreateFolderResponse response = new APICreateFolderResponse(item, false, 404, "User not found.");
			return response;
		}

		// get metadata of the parent item
		APIGetMetadata parentResponse = this.getFolderContent(user, item.getParentId(), false);
		ItemMetadata parentMetadata = parentResponse.getItemMetadata();

		// if it is the root, get the default workspace
		if (parentMetadata.isRoot()) {

			try {
				Workspace workspace = workspaceDAO.getDefaultWorkspaceByUserId(user.getId());
				parentMetadata.setWorkspaceId(workspace.getId());
			} catch (DAOException e) {
				logger.error(e);
				APICreateFolderResponse response = new APICreateFolderResponse(item, false, 404, "Workspace not found.");
				return response;
			}
		} else {

			if (!parentMetadata.isFolder()) {
				return new APICreateFolderResponse(item, false, 400, "Parent must be a folder, not a file.");
			}

			item.setParentVersion(parentMetadata.getVersion());
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
	public APIRestoreMetadata restoreMetadata(User user, ItemMetadata item) {
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
	public APIDeleteResponse deleteItem(User user, ItemMetadata item) {
		List<ItemMetadata> filesToDelete;

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			return new APIDeleteResponse(null, false, 404, "User not found.");
		}

		// Get user workspaces
		try {
			List<Workspace> workspaces = workspaceDAO.getByUserId(user.getId());
			user.setWorkspaces(workspaces);
		} catch (DAOException e) {
			logger.error(e);
			return new APIDeleteResponse(null, false, 404, "No workspaces found for the user.");
		}

		// check that the given file ID exists
		try {
			filesToDelete = itemDao.getItemsById(item.getId());
		} catch (DAOException e) {
			return new APIDeleteResponse(null, false, 404, "File or folder not found");
		}
		if (filesToDelete.isEmpty()) {
			return new APIDeleteResponse(null, false, 404, "File or folder not found.");
		}

		// check if it's a file or a folder
		if (filesToDelete.get(0).isFolder() != item.isFolder()) {
			return new APIDeleteResponse(null, false, 400, "Type missmatch (file and folder)");
		}

		// check if the user has permission on the file and parent
		boolean permission = false;
		for (Workspace w : user.getWorkspaces()) {
			if (w.getId().equals(filesToDelete.get(0).getWorkspaceId())) {
				permission = true;
			}
		}
		if (!permission) {
			return new APIDeleteResponse(null, false, 403, "You are not allowed to deleted this file");
		}

		Workspace workspace = new Workspace(filesToDelete.get(0).getWorkspaceId());

		APIDeleteResponse response;
		try {
			response = deleteItemsAPI(user, workspace, filesToDelete);
		} catch (DAOException e) {
			logger.error(e.toString(), e);
			response = new APIDeleteResponse(null, false, e.getError().getCode(), e.getMessage());
		}

		return response;
	}

	@Override
	public APIGetVersions getVersions(User user, ItemMetadata item) {
		ItemMetadata serverItem = null;

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			return new APIGetVersions(null, false, 404, "User not found.");
		}

		// Get user workspaces
		try {
			List<Workspace> workspaces = workspaceDAO.getByUserId(user.getId());
			user.setWorkspaces(workspaces);
		} catch (DAOException e) {
			logger.error(e);
			return new APIGetVersions(null, false, 404, "No workspaces found for the user.");
		}

		// check that the given file ID exists
		try {
			serverItem = itemDao.findItemVersionsById(item.getId());
		} catch (DAOException e) {
			return new APIGetVersions(null, false, 404, "File or folder not found");
		}

		// check if it's a file or a folder
		if (serverItem.isFolder()) {
			return new APIGetVersions(null, false, 400, "Incorrect file type. Must be a file, not a folder.");
		}

		APIGetVersions response = new APIGetVersions(serverItem, true, 0, "");
		return response;
	}

	@Override
	public APIGetWorkspaceInfoResponse getWorkspaceInfo(User user, ItemMetadata item) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (DAOException e) {
			logger.error(e);
			return new APIGetWorkspaceInfoResponse(null, false, 404, "User not found.");
		}

		// Get user workspaces
		try {
			List<Workspace> workspaces = workspaceDAO.getByUserId(user.getId());
			user.setWorkspaces(workspaces);
		} catch (DAOException e) {
			logger.error(e);
			return new APIGetWorkspaceInfoResponse(null, false, 404, "No workspaces found for the user.");
		}

		// get the workspace

		Workspace workspace;
		if (item.getId() == null) {
			try {
				workspace = workspaceDAO.getDefaultWorkspaceByUserId(user.getId());
			} catch (DAOException e) {
				return new APIGetWorkspaceInfoResponse(null, false, 404, "Workspace not found");
			}
		} else {
			try {
				workspace = workspaceDAO.getByItemId(item.getId());
			} catch (DAOException e) {
				return new APIGetWorkspaceInfoResponse(null, false, 404, "Workspace not found");
			}
		}

		// check if the user has permission on the file and parent
		boolean permission = false;
		for (Workspace w : user.getWorkspaces()) {
			if (item.getId() == null || w.getId().equals(workspace.getId())) {
				permission = true;
				break;
			}
		}
		if (!permission) {
			return new APIGetWorkspaceInfoResponse(null, false, 403, "You are not allowed to access this file");
		}

		APIGetWorkspaceInfoResponse response = new APIGetWorkspaceInfoResponse(workspace, true, 0, "");
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

	private void saveNewItemAPI(User user, ItemMetadata itemToSave, ItemMetadata parent) throws DAOException {

		itemToSave.setWorkspaceId(parent.getWorkspaceId());
		Workspace workspace = new Workspace(parent.getWorkspaceId());

		List<ItemMetadata> objects = new ArrayList<ItemMetadata>();
		objects.add(itemToSave);

		this.doCommit(user, workspace, apiDevice, objects);

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

	private APIDeleteResponse deleteItemsAPI(User user, Workspace workspace, List<ItemMetadata> filesToDelete)
			throws DAOException {

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

		List<CommitInfo> commitResponse = this.doCommit(user, workspace, apiDevice, items);

		if (!commitResponse.isEmpty()) {
			fileToDelete = commitResponse.get(0).getMetadata();
			success = true;
		}

		APIDeleteResponse response = new APIDeleteResponse(fileToDelete, success, 0, "");
		return response;
	}

}
