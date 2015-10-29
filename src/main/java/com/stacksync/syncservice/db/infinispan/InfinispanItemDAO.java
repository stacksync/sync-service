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
public interface InfinispanItemDAO extends Remote {

    public ItemRMI findById(Long id) throws RemoteException;

    public void add(ItemRMI item) throws RemoteException;

    public void update(ItemRMI item) throws RemoteException;

    public void put(ItemRMI item) throws RemoteException;

    public void delete(Long id) throws RemoteException;

    // ItemMetadata information
    public List<ItemMetadataRMI> getItemsByWorkspaceId(UUID workspaceId)
            throws RemoteException;

    public List<ItemMetadataRMI> getItemsById(Long id) throws RemoteException;

    public ItemMetadataRMI findById(Long id, Boolean includeList, Long version,
            Boolean includeDeleted, Boolean includeChunks)
            throws RemoteException;

    public ItemMetadataRMI findByUserId(UUID serverUserId,
            Boolean includeDeleted) throws RemoteException;

    public ItemMetadataRMI findItemVersionsById(Long id) throws RemoteException;

    public List<String> migrateItem(Long itemId, UUID workspaceId)
            throws RemoteException;

}