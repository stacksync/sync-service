/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.stacksync.syncservice.db.infinispan.models.ItemVersionRMI;
import com.stacksync.syncservice.db.infinispan.models.ChunkRMI;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface InfinispanItemVersionDAO extends Remote {

    public ItemMetadata findByItemIdAndVersion(Long id, Long version)
            throws RemoteException;

    public void add(ItemVersionRMI itemVersion) throws RemoteException;

    public void insertChunk(Long itemVersionId, Long chunkId, Integer order)
            throws RemoteException;

    public void insertChunks(long itemId, List<ChunkRMI> chunks, long itemVersionId)
            throws RemoteException;

    public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException;

    public void update(ItemVersionRMI itemVersion) throws RemoteException;

    public void delete(ItemVersionRMI itemVersion) throws RemoteException;
}