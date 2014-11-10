package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class ItemVersionDAORMISer extends UnicastRemoteObject implements
		ItemVersionDAORMIIfc {

	List<ItemVersionRMI> llistat;

	public ItemVersionDAORMISer() throws RemoteException {
		llistat = new ArrayList<ItemVersionRMI>();
	}

	@Override
	public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version)
			throws RemoteException {

		ItemMetadataRMI metadata = null;

		for (ItemVersionRMI iv : llistat) {
			if (iv.getVersion().equals(version)
					&& iv.getItem().getId().equals(id)) {
				metadata = new ItemMetadataRMI();
			}
		}

		return metadata;
	}

	@Override
	public void add(ItemVersionRMI itemVersion) throws RemoteException {
		if (!itemVersion.isValid()) {
			throw new IllegalArgumentException("Item version attributes not set");
		}
		
		boolean exist = false;
		
		for (ItemVersionRMI iv: llistat){
			if (iv.getId() == itemVersion.getId()){
				exist = true;
			}
		}
		
		if (!exist) {
			llistat.add(itemVersion);
			System.out.println("ADDED");
		} else
			System.out.println("EXISTING ITEM VERSION ID");
	}

	@Override
	public void update(ItemVersionRMI itemVersion) throws RemoteException {
		if (itemVersion.getId() == null || !itemVersion.isValid()) {
			throw new IllegalArgumentException("Device attributes not set");
		}
		
		boolean exist = false;
		ItemVersionRMI iv1 = null;
		
		for (ItemVersionRMI iv: llistat){
			if (iv.getId() == itemVersion.getId()){
				exist = true;
				iv1 = iv;
			}
		}
		
		if (exist) {
			llistat.remove(iv1);
			llistat.add(itemVersion);
			System.out.println("UPDATED");
		} else
			System.out.println("ITEM VERSION ID DOESN'T EXIST");
	}

	@Override
	public void delete(ItemVersionRMI itemVersion) throws RemoteException {
		boolean exist = false;
		ItemVersionRMI iv1 = null;
		
		for (ItemVersionRMI iv: llistat){
			if (iv.getId() == itemVersion.getId()){
				exist = true;
				iv1 = iv;
			}
		}
		
		if (exist) {
			llistat.remove(iv1);
			System.out.println("DELETED");
		} else
			System.out.println("ITEM VERSION ID DOESN'T EXIST");
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
