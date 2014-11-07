/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db;

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author javigd
 */
public interface ABEItemDAO {
    
        public List<ItemMetadata> getABEItemsByWorkspaceId(UUID workspaceId) throws DAOException;
    
}
