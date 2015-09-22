/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db;

import com.stacksync.commons.models.SyncMetadata;
import com.stacksync.commons.models.abe.ABEMetaComponent;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author javigd
 */
public interface ABEItemDAO extends ItemDAO {
    
        // ABE ItemMetadata information
        public List<SyncMetadata> getABEItemsByWorkspaceId(UUID workspaceId) throws DAOException;
                 
        public ArrayList<Long> getItemsIDWithAttributeInWorkspace(UUID workspace, String attribute) throws DAOException;
        
        public void setNotUpdatedItemInWorkspace(Long itemId) throws DAOException;
        
        public void setUpdatedItem(Long itemId) throws DAOException;
        
        public HashMap<Long,ArrayList<ABEMetaComponent>> getNotUpdatedABEComponentsInWorkspace(UUID workspaceId) throws DAOException;

        public void setUpdatedABEComponents(HashMap<Long,ArrayList<ABEMetaComponent>> components) throws DAOException;
}
