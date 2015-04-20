package com.stacksync.syncservice.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.infinispan.models.ChunkRMI;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemVersionRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.UserWorkspaceRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.rpc.messages.APICreateFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIDeleteResponse;
import com.stacksync.syncservice.rpc.messages.APIGetFolderMembersResponse;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.messages.APIGetVersions;
import com.stacksync.syncservice.rpc.messages.APIGetWorkspaceInfoResponse;
import com.stacksync.syncservice.rpc.messages.APIRestoreMetadata;
import com.stacksync.syncservice.rpc.messages.APIShareFolderResponse;
import com.stacksync.syncservice.rpc.messages.APIUnshareFolderResponse;
import com.stacksync.syncservice.util.Constants;
import java.rmi.RemoteException;
import java.util.UUID;
import java.util.logging.Level;

public class SQLAPIHandler extends Handler implements APIHandler {

	private static final Logger logger = Logger.getLogger(SQLAPIHandler.class
			.getName());

	private DeviceRMI apiDevice = new DeviceRMI(Constants.API_DEVICE_ID);

	public SQLAPIHandler(ConnectionPool pool) throws SQLException,
			NoStorageManagerAvailable,
			Exception {
		super(pool);
	}

	@Override
	public APIGetMetadata getMetadata(UserRMI user, Long fileId,
			Boolean includeChunks, Long version, Boolean isFolder) {

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
				List<UserRMI> users = this.userDao.findByItemId(fileId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.itemDao.findById(fileId, false, version,
						false, includeChunks);
			}

			if (responseObject.isFolder() != isFolder) {
				throw new DAOException(DAOError.FILE_NOT_FOUND);
			}

			success = true;

		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		APIGetMetadata response = new APIGetMetadata(responseObject, success,
				errorCode, description);
		return response;
	}

	public APIGetMetadata getFolderContent(UserRMI user, Long folderId,
			Boolean includeDeleted) {

		ItemMetadata responseObject = null;
		Integer errorCode = 0;
		Boolean success = false;
		String description = "";

		try {

			if (folderId == null) {
				// retrieve metadata from the root folder
				responseObject = this.itemDao.findByUserId(user.getId(),
						includeDeleted);
			} else {

				// check if user has permission on this file
				List<UserRMI> users = this.userDao.findByItemId(folderId);

				if (users.isEmpty()) {
					throw new DAOException(DAOError.FILE_NOT_FOUND);
				}

				if (!userHasPermission(user, users)) {
					throw new DAOException(DAOError.USER_NOT_AUTHORIZED);
				}

				responseObject = this.itemDao.findById(folderId, true, null,
						includeDeleted, false);
			}

			success = true;

		} catch (DAOException e) {
			description = e.getError().getMessage();
			errorCode = e.getError().getCode();
			logger.error(e.toString(), e);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		APIGetMetadata response = new APIGetMetadata(responseObject, success,
				errorCode, description);
		return response;
	}

	@Override
	public APICommitResponse createFile(UserRMI user, ItemMetadata fileToSave) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get user workspaces
		try {
			List<WorkspaceRMI> workspaces = workspaceDAO.getByUserId(user.getId());
                        List<UUID> workspaceUUIDs = new ArrayList<UUID>();
                        for (WorkspaceRMI workspaceInList : workspaces) {
                            workspaceUUIDs.add(workspaceInList.getId());
                        }
			user.setWorkspaces(workspaceUUIDs);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		boolean includeList = true;
		Long version = null;
		boolean includeDeleted = false;
		boolean includeChunks = false;

		// check that the given parent ID exists
		ItemMetadata parent = null;
		if (fileToSave.getParentId() != null) {
			try {
				parent = itemDao.findById(fileToSave.getParentId(),
						includeList, version, includeDeleted, includeChunks);
				fileToSave.setParentVersion(parent.getVersion());

				// check if parent is a folder
				if (!parent.isFolder()) {
					return new APICommitResponse(fileToSave, false, 400,
							"Parent must be a folder, not a file.");
				}

			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		} else {
			try {
				parent = this.itemDao
						.findByUserId(user.getId(), includeDeleted);
				WorkspaceRMI parentWorkspace = workspaceDAO
						.getDefaultWorkspaceByUserId(user.getId());
				parent.setWorkspaceId(parentWorkspace.getId());
			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		}

		// check if the user has permission on the file and parent
		boolean permissionParent = false;
		for (UUID w : user.getWorkspaces()) {
			if (parent.isRoot() || w.equals(parent.getWorkspaceId())) {
				permissionParent = true;
				break;
			}
		}
		if (!permissionParent) {
			return new APICommitResponse(fileToSave, false, 403,
					"You are not allowed to modify this file");
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
			responseAPI = new APICommitResponse(fileToSave, false, 500,
					e.toString());
		}

		return responseAPI;
	}

	@Override
	public APICommitResponse updateData(UserRMI user, ItemMetadata fileToUpdate) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get user workspaces
		try {
			List<WorkspaceRMI> workspaces = workspaceDAO.getByUserId(user.getId());
                        List<UUID> workspaceUUIDs = new ArrayList<UUID>();
                        for (WorkspaceRMI workspaceInList : workspaces) {
                            workspaceUUIDs.add(workspaceInList.getId());
                        }
			user.setWorkspaces(workspaceUUIDs);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		boolean includeList = true;
		Long version = null;
		boolean includeDeleted = false;
		boolean includeChunks = false;

		// check that the given file ID exists
		ItemMetadata file = null;
		try {
			file = itemDao.findById(fileToUpdate.getId(), includeList, version,
					includeDeleted, includeChunks);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// check if the user has permission on the file and parent
		boolean permission = false;
		for (UUID w : user.getWorkspaces()) {
			if (w.equals(file.getWorkspaceId())) {
				permission = true;
			}
		}
		if (!permission) {
			return new APICommitResponse(fileToUpdate, false, 403,
					"You are not allowed to modify this file");
		}

		// update file attributes

		file.setMimetype(fileToUpdate.getMimetype());
		file.setChecksum(fileToUpdate.getChecksum());
		file.setSize(fileToUpdate.getSize());
		file.setChunks(fileToUpdate.getChunks());
		file.setVersion(file.getVersion() + 1L);
		file.setModifiedAt(new Date());
		file.setStatus(Status.CHANGED.toString());

		// Commit the file
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(file);

		WorkspaceRMI workspace = new WorkspaceRMI(file.getWorkspaceId());

		try {
			this.doCommit(user, workspace, apiDevice, items);
		} catch (DAOException e) {
			return new APICommitResponse(fileToUpdate, false, e.getError()
					.getCode(), e.getMessage());
		} catch (Exception ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		APICommitResponse responseAPI = new APICommitResponse(file, true, 0, "");
		return responseAPI;
	}

	@Override
	public APICommitResponse updateMetadata(UserRMI user, ItemMetadata fileToUpdate) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get user workspaces
		try {
			List<WorkspaceRMI> workspaces = workspaceDAO.getByUserId(user.getId());
                        List<UUID> workspaceUUIDs = new ArrayList<UUID>();
                        for (WorkspaceRMI workspaceInList : workspaces) {
                            workspaceUUIDs.add(workspaceInList.getId());
                        }
			user.setWorkspaces(workspaceUUIDs);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		boolean includeList = true;
		Long version = null;
		boolean includeDeleted = false;
		boolean includeChunks = true;

		// check that the given file ID exists
		ItemMetadata file = null;
		try {
			file = itemDao.findById(fileToUpdate.getId(), includeList, version,
					includeDeleted, includeChunks);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// check that the given parent ID exists
		ItemMetadata parent = null;
		if (fileToUpdate.getParentId() != null) {
			try {
				parent = itemDao.findById(fileToUpdate.getParentId(),
						includeList, version, includeDeleted, includeChunks);

				// check if parent is a folder
				if (!parent.isFolder()) {
					return new APICommitResponse(fileToUpdate, false, 400,
							"Parent must be a folder, not a file.");
				}

			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		} else {
			try {
				parent = this.itemDao
						.findByUserId(user.getId(), includeDeleted);
			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		}

		// check if the user has permission on the file and parent
		boolean permissionFile = false;
		boolean permissionParent = false;
		for (UUID w : user.getWorkspaces()) {
			if (w.equals(file.getWorkspaceId())) {
				permissionFile = true;
			}
			if (parent.isRoot() || w.equals(parent.getWorkspaceId())) {
				permissionParent = true;
			}
		}
		if (!permissionFile || !permissionParent) {
			return new APICommitResponse(fileToUpdate, false, 403,
					"You are not allowed to modify this file");
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
		file.setModifiedAt(new Date());
		file.setStatus(Status.RENAMED.toString());

		// Commit the file
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(file);

		WorkspaceRMI workspace = new WorkspaceRMI(file.getWorkspaceId());

		try {
			this.doCommit(user, workspace, apiDevice, items);
		} catch (DAOException e) {
			return new APICommitResponse(fileToUpdate, false, e.getError()
					.getCode(), e.getMessage());
		} catch (Exception ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		APICommitResponse responseAPI = new APICommitResponse(file, true, 0, "");
		return responseAPI;
	}

	@Override
	public APICreateFolderResponse createFolder(UserRMI user, ItemMetadata item) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// get metadata of the parent item
		APIGetMetadata parentResponse = this.getFolderContent(user,
				item.getParentId(), false);
		ItemMetadata parentMetadata = parentResponse.getItemMetadata();

		// if it is the root, get the default workspace
		if (parentMetadata.isRoot()) {

			try {
				WorkspaceRMI workspace = workspaceDAO
						.getDefaultWorkspaceByUserId(user.getId());
				parentMetadata.setWorkspaceId(workspace.getId());
			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		} else {

			if (!parentMetadata.isFolder()) {
				return new APICreateFolderResponse(item, false, 400,
						"Parent must be a folder, not a file.");
			}

			item.setParentVersion(parentMetadata.getVersion());
		}

		String folderName = item.getFilename();
		List<ItemMetadata> files = parentMetadata.getChildren();

		// check if there exists a folder with the same name
		ItemMetadata object = null;
		for (ItemMetadata file : files) {
			if (file.getFilename().equals(folderName)
					&& !file.getStatus().equals("DELETED")) {
				object = file;
				break;
			}
		}

		if (object != null) {
			APICreateFolderResponse response = new APICreateFolderResponse(
					object, false, 400, "Folder already exists.");
			return response;
		}

		boolean succeded = false;
            try {
                succeded = this.createNewFolder(user, item, parentMetadata);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		if (!succeded) {
			APICreateFolderResponse response = new APICreateFolderResponse(
					item, false, 500, "Item could not be committed.");
			return response;
		}

		APICreateFolderResponse responseAPI = new APICreateFolderResponse(item,
				true, 0, "");
		return responseAPI;
	}

	@Override
	public APIRestoreMetadata restoreMetadata(UserRMI user, ItemMetadata item) {
		try {

			ItemRMI serverItem = itemDao.findById(item.getId());
			ItemMetadata lastObjectVersion = itemDao.findById(item.getId(),
					false, null, false, false);
			if (serverItem != null && lastObjectVersion != null) {

				ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(
						serverItem.getId(), item.getVersion());

				ItemVersionRMI restoredObject = new ItemVersionRMI(metadata);

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
						for (ChunkRMI chunk : restoredObject.getChunks()) {
							chunks.add(chunk.getClientChunkName());
						}
						this.createChunks(chunks, restoredObject);
					}

					serverItem.setLatestVersionNumber(restoredObject.getVersion());
					itemDao.put(serverItem);

					item.setChecksum(restoredObject.getChecksum());
					item.setChunks(chunks);
					item.setModifiedAt(restoredObject.getModifiedAt());
					item.setDeviceId(restoredObject.getDevice().getId());
					//item.setFilename(restoredObject.getItem().getFilename());
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
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
	}

	@Override
	public APIDeleteResponse deleteItem(UserRMI user, ItemMetadata item) {
		List<ItemMetadata> filesToDelete = null;

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get user workspaces
		try {
			List<WorkspaceRMI> workspaces = workspaceDAO.getByUserId(user.getId());
                        List<UUID> workspaceUUIDs = new ArrayList<UUID>();
                        for (WorkspaceRMI workspaceInList : workspaces) {
                            workspaceUUIDs.add(workspaceInList.getId());
                        }
			user.setWorkspaces(workspaceUUIDs);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// check that the given file ID exists
		try {
			filesToDelete = itemDao.getItemsById(item.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
		if (filesToDelete.isEmpty()) {
			return new APIDeleteResponse(null, false, 404,
					"File or folder not found.");
		}

		// check if it's a file or a folder
		if (filesToDelete.get(0).isFolder() != item.isFolder()) {
			return new APIDeleteResponse(null, false, 400,
					"Type missmatch (file and folder)");
		}

		// check if the user has permission on the file and parent
		boolean permission = false;
		for (UUID w : user.getWorkspaces()) {
			if (w.equals(filesToDelete.get(0).getWorkspaceId())) {
				permission = true;
			}
		}
		if (!permission) {
			return new APIDeleteResponse(null, false, 403,
					"You are not allowed to deleted this file");
		}

		WorkspaceRMI workspace = new WorkspaceRMI(filesToDelete.get(0)
				.getWorkspaceId());

		APIDeleteResponse response = null;
		try {
			response = deleteItemsAPI(user, workspace, filesToDelete);
		} catch (DAOException e) {
			logger.error(e.toString(), e);
			response = new APIDeleteResponse(null, false, e.getError()
					.getCode(), e.getMessage());
		} catch (Exception ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		return response;
	}

	@Override
	public APIGetVersions getVersions(UserRMI user, ItemMetadata item) {
		ItemMetadata serverItem = null;

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get user workspaces
		try {
			List<WorkspaceRMI> workspaces = workspaceDAO.getByUserId(user.getId());
                        List<UUID> workspaceUUIDs = new ArrayList<UUID>();
                        for (WorkspaceRMI workspaceInList : workspaces) {
                            workspaceUUIDs.add(workspaceInList.getId());
                        }
			user.setWorkspaces(workspaceUUIDs);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// check that the given file ID exists
		try {
			serverItem = itemDao.findItemVersionsById(item.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// check if it's a file or a folder
		if (serverItem.isFolder()) {
			return new APIGetVersions(null, false, 400,
					"Incorrect file type. Must be a file, not a folder.");
		}

		APIGetVersions response = new APIGetVersions(serverItem, true, 0, "");
		return response;
	}

	@Override
	public APIShareFolderResponse shareFolder(UserRMI user, ItemRMI item,
			List<String> emails) {

		APIShareFolderResponse response;

		WorkspaceRMI workspace;
		try {
			workspace = this.doShareFolder(user, emails, item, false);
			response = new APIShareFolderResponse(workspace, true, 0, "");
		} catch (ShareProposalNotCreatedException e) {
			response = new APIShareFolderResponse(null, false, 400,
					e.getMessage());
		} catch (UserNotFoundException e) {
			response = new APIShareFolderResponse(null, false, 404,
					e.getMessage());
		}

		return response;
	}

	@Override
	public APIUnshareFolderResponse unshareFolder(UserRMI user, ItemRMI item,
			List<String> emails) {

		APIUnshareFolderResponse response;
		UnshareData infoUnshare;

		try {
			infoUnshare = this.doUnshareFolder(user, emails, item, false);
			response = new APIUnshareFolderResponse(infoUnshare.getWorkspace(),
					infoUnshare.getUsersToRemove(), infoUnshare.isUnshared(),
					true, 0, "");
		} catch (ShareProposalNotCreatedException e) {
			response = new APIUnshareFolderResponse(null, null, false, false, 400,
					e.getMessage());
		} catch (UserNotFoundException e) {
			response = new APIUnshareFolderResponse(null, null, false, false, 404,
					e.getMessage());
		}

		return response;
	}

	@Override
	public APIGetFolderMembersResponse getFolderMembers(UserRMI user, ItemRMI item) {

		/*APIGetFolderMembersResponse response;

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get folder metadata
		try {
			item = itemDao.findById(item.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		if (item == null || !item.isFolder()) {
			return new APIGetFolderMembersResponse(null, false, 404,
					"No folder found with the given ID.");
		}

		List<UserWorkspaceRMI> members;
                members = this.doGetWorkspaceMembers(user, item.getWorkspace());

		response = new APIGetFolderMembersResponse(members, true, 0, "");

		return response;
                        */
            return null;
	}

	@Override
	public APIGetWorkspaceInfoResponse getWorkspaceInfo(UserRMI user,
			ItemMetadata item) {

		// Check the owner
		try {
			user = userDao.findById(user.getId());
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// Get user workspaces
		try {
			List<WorkspaceRMI> workspaces = workspaceDAO.getByUserId(user.getId());
                        List<UUID> workspaceUUIDs = new ArrayList<UUID>();
                        for (WorkspaceRMI workspaceInList : workspaces) {
                            workspaceUUIDs.add(workspaceInList.getId());
                        }
			user.setWorkspaces(workspaceUUIDs);
		} catch (RemoteException ex) {
                java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

		// get the workspace

		WorkspaceRMI workspace = null;
		if (item.getId() == null) {
			try {
				workspace = workspaceDAO.getDefaultWorkspaceByUserId(user
						.getId());
			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		} else {
			try {
				workspace = workspaceDAO.getByItemId(item.getId());
			} catch (RemoteException ex) {
                        java.util.logging.Logger.getLogger(SQLAPIHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
		}

		// check if the user has permission on the file and parent
		boolean permission = false;
		for (UUID w : user.getWorkspaces()) {
			if (item.getId() == null || w.equals(workspace.getId())) {
				permission = true;
				break;
			}
		}
		if (!permission) {
			return new APIGetWorkspaceInfoResponse(null, false, 403,
					"You are not allowed to access this file");
		}

		APIGetWorkspaceInfoResponse response = new APIGetWorkspaceInfoResponse(
				workspace, true, 0, "");
		return response;
	}

	private boolean userHasPermission(UserRMI user, List<UserRMI> users) {
		boolean hasPermission = false;
		for (UserRMI u : users) {
			if (u.getId().equals(user.getId())) {
				hasPermission = true;
				break;
			}
		}
		return hasPermission;
	}

	private void saveNewItemAPI(UserRMI user, ItemMetadata itemToSave,
			ItemMetadata parent) throws DAOException, Exception {

		itemToSave.setWorkspaceId(parent.getWorkspaceId());
		WorkspaceRMI workspace = new WorkspaceRMI(parent.getWorkspaceId());

		List<ItemMetadata> objects = new ArrayList<ItemMetadata>();
		objects.add(itemToSave);

		this.doCommit(user, workspace, apiDevice, objects);

	}

	private boolean createNewFolder(UserRMI user, ItemMetadata item,
			ItemMetadata parent) throws Exception {

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

		WorkspaceRMI workspace = new WorkspaceRMI(item.getWorkspaceId());

		try {
			List<CommitInfo> commitInfo = this.doCommit(user, workspace,
					apiDevice, items);
			return commitInfo.get(0).isCommitSucceed();
		} catch (DAOException e) {
			logger.error(e);
			return false;
		}
	}

	private void createChunks(List<String> chunksString,
			ItemVersionRMI objectVersion) throws IllegalArgumentException,
			DAOException,
			RemoteException {

		if (chunksString.size() > 0) {
			List<ChunkRMI> chunks = new ArrayList<ChunkRMI>();
			int i = 0;

			for (String chunkName : chunksString) {
				chunks.add(new ChunkRMI(chunkName, i));
				i++;
			}

			itemVersionDao.insertChunks(objectVersion.getItemId(), chunks, objectVersion.getId());
		}
	}

	private APIDeleteResponse deleteItemsAPI(UserRMI user, WorkspaceRMI workspace,
			List<ItemMetadata> filesToDelete) throws DAOException, Exception {

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

		List<CommitInfo> commitResponse = this.doCommit(user, workspace,
				apiDevice, items);

		if (!commitResponse.isEmpty()) {
			fileToDelete = commitResponse.get(0).getMetadata();
			success = true;
		}

		APIDeleteResponse response = new APIDeleteResponse(fileToDelete,
				success, 0, "");
		return response;
	}

}
