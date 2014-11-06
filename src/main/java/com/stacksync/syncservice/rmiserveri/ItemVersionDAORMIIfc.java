package com.stacksync.syncservice.rmiserveri;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.stacksync.syncservice.rmiclient.*;

public interface ItemVersionDAORMIIfc extends Remote {

	public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version) throws RemoteException;

	public void add(ItemVersionRMI itemVersion) throws RemoteException;

	public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws RemoteException;

	public void insertChunks(List<ChunkRMI> chunks, long itemVersionId) throws RemoteException;

	public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException;

	public void update(ItemVersionRMI itemVersion) throws RemoteException;

	public void delete(ItemVersionRMI itemVersion) throws RemoteException;
}
