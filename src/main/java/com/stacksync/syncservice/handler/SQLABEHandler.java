/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.handler;

import com.stacksync.commons.models.ABEItemMetadata;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author javigd
 */
public class SQLABEHandler extends SQLSyncHandler {

    private static final Logger logger = Logger.getLogger(SQLABEHandler.class.getName());

    public SQLABEHandler(ConnectionPool pool) throws SQLException, NoStorageManagerAvailable {
        super(pool);
    }

    @Override
    public List<ItemMetadata> doGetChanges(User user, Workspace workspace) {
        List<ItemMetadata> responseObjects = new ArrayList<ItemMetadata>();
        
        try {
            responseObjects = itemDao.getABEItemsByWorkspaceId(workspace.getId());
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }

        return responseObjects;
    }

}
