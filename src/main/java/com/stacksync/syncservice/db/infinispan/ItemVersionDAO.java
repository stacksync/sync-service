/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.infinispan.models.ChunkRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemVersionRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface ItemVersionDAO extends Remote {

    ItemMetadataRMI findByItemIdAndVersion(Long id, Long version)
            throws RemoteException;

    void add(ItemVersionRMI itemVersion) throws RemoteException;

    void insertChunk(Long itemVersionId, Long chunkId, Integer order)
            throws RemoteException;

    void insertChunks(ItemVersionRMI versionRMI, List<ChunkRMI> chunks)
            throws RemoteException;

    List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException;

    void update(ItemVersionRMI itemVersion) throws RemoteException;

    void delete(ItemVersionRMI itemVersion) throws RemoteException;
}