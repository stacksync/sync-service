package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class ItemVersionDAORMISer extends UnicastRemoteObject implements
		ItemVersionDAORMIIfc {

	public ItemVersionDAORMISer() throws RemoteException {
		super();
	}

	@Override
	public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version)
			throws RemoteException {

		ItemMetadataRMI metadata = null;

		return metadata;
	}

	@Override
	public void add(ItemVersionRMI itemVersion) throws RemoteException {

	}

	@Override
	public void update(ItemVersionRMI itemVersion) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(ItemVersionRMI itemVersion) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void insertChunk(Long itemVersionId, Long chunkId, Integer order)
			throws RemoteException {

	}

	@Override
	public void insertChunks(List<ChunkRMI> chunks, long itemVersionId)
			throws RemoteException {

	}

	@Override
	public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException {
		List<ChunkRMI> chunks = new ArrayList<ChunkRMI>();

		return chunks;
	}

}
