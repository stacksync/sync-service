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
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIUserMetadata;
import com.stacksync.syncservice.util.Constants;

public class SQLAPIHandler extends Handler implements APIHandler {

	private static final Logger logger = Logger.getLogger(SQLAPIHandler.class.getName());
	
	private Device apiDevice = new Device(Constants.API_DEVICE_ID);
	
	public SQLAPIHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
		super(pool);
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
	
}
