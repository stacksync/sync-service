package com.stacksync.syncservice.handler;

import com.ast.cloudABE.kpabe.AttributeUpdate;
import com.ast.cloudABE.kpabe.AttributeUpdateForUser;
import com.ast.cloudABE.kpabe.KPABE;
import com.ast.cloudABE.kpabe.RevokeMessage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.commons.models.SyncMetadata;
import com.stacksync.commons.models.abe.ABEMetaComponent;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;
import com.stacksync.syncservice.exceptions.dao.NoRowsAffectedDAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.util.Config;
import java.util.HashMap;
import java.util.LinkedList;

public class SQLSyncHandler extends Handler implements SyncHandler {

	private static final Logger logger = Logger.getLogger(SQLSyncHandler.class.getName());	

	public SQLSyncHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
		super(pool);
	}

	@Override
	public List<SyncMetadata> doGetChanges(User user, Workspace workspace) {
		List<SyncMetadata> responseObjects = null;

		try {
                        if(workspaceDAO.getById(workspace.getId()).isAbeEncrypted()) {
                            
                            HashMap<String,LinkedList<AttributeUpdate>> attributeVersions = workspaceDAO.getAttributeVersions(workspace.getId());
                            KPABE kpabe = new KPABE (Config.getCurvePath());

                            HashMap<Long,ArrayList<ABEMetaComponent>> abeFileComponents = abeItemDao.getNotUpdatedABEComponentsInWorkspace(workspace.getId());
                            
                            for(Long fileId:abeFileComponents.keySet()){
                                for(ABEMetaComponent component:abeFileComponents.get(fileId)){
                                    
                                    if(attributeVersions.get(component.getAttributeId())!=null && component.getVersion()<attributeVersions.get(component.getAttributeId()).size()+1){
                                        byte[] updatedComponent = kpabe.updateAttributeForFile(component.getVersion()-1, component.getEncryptedPKComponent(), attributeVersions.get(component.getAttributeId()));
                                    
                                        //size()+1 as there isn't a reencryption key for the newest version.
                                        component.setEncryptedPKComponent(updatedComponent);
                                        component.setVersion(new Long(attributeVersions.size()+1));
                                    }

                                }
                            }
                            
                            abeItemDao.setUpdatedABEComponents(abeFileComponents);
                                                            
                            responseObjects = abeItemDao.getABEItemsByWorkspaceId(workspace.getId());    
                            
                        } else {
                                responseObjects = itemDao.getItemsByWorkspaceId(workspace.getId());
                        }

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
