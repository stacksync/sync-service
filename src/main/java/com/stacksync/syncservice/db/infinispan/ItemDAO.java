/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface ItemDAO extends Remote {

    ItemRMI findById(Long id) throws RemoteException;

    void add(ItemRMI item) throws RemoteException;

    void update(ItemRMI item) throws RemoteException;

    void delete(Long id) throws RemoteException;

    // ItemMetadata information
    List<ItemMetadataRMI> getItemsByWorkspaceId(UUID workspaceId)
            throws RemoteException;

    List<ItemMetadataRMI> getItemsById(Long id) throws RemoteException;

    ItemMetadataRMI findById(Long id, Boolean includeList, Long version,
            Boolean includeDeleted, Boolean includeChunks)
            throws RemoteException;

    ItemMetadataRMI findByUserId(UUID serverUserId,
            Boolean includeDeleted) throws RemoteException;

    ItemMetadataRMI findItemVersionsById(Long id) throws RemoteException;

    List<String> migrateItem(Long itemId, UUID workspaceId)
            throws RemoteException;

}