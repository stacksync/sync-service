package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class ItemDAORMISer extends UnicastRemoteObject implements ItemDAORMIIfc {

	List<ItemRMI> llistat;

	public ItemDAORMISer() throws RemoteException {
		llistat = new ArrayList<ItemRMI>();
	}

	@Override
	public ItemRMI findById(Long item1ID) throws RemoteException {
		ItemRMI item = null;

		for (ItemRMI i : llistat) {
			if (i.getId().equals(item1ID)) {
				item = i;
			}
		}

		return item;
	}

	@Override
	public void add(ItemRMI item) throws RemoteException {
		if (!item.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}
		if (findById(item.getId()) == null) {
			llistat.add(item);
			System.out.println("ADDED");
		} else
			System.out.println("EXISTING ITEM ID");
	}

	@Override
	public void put(ItemRMI item) throws RemoteException {
		if (findById(item.getId()) == null) {
			add(item);
		} else
			update(item);
	}

	@Override
	public void update(ItemRMI item) throws RemoteException {
		if (item.getId() == null || !item.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}

		Long parentId = item.getParentId();
		// If id == 0 means parent is null!
		if (parentId != null && parentId == 0) {
			parentId = null;
		}

		if (findById(item.getId()) != null) {
			llistat.remove(findById(item.getId()));
			llistat.add(item);
			System.out.println("UPDATED");
		} else
			System.out.println("ITEM ID DOESN'T EXIST");
	}

	@Override
	public void delete(Long id) throws RemoteException {
		// TODO Auto-generated method stub

		if (findById(id) == null) {
			llistat.remove(findById(id));
			System.out.println("REMOVED");
		} else
			System.out.println("ITEM ID DOESN'T EXIST");
	}

	@Override
	public List<ItemMetadataRMI> getItemsByWorkspaceId(UUID workspaceId)
			throws RemoteException {

		List<ItemMetadataRMI> items = null;

		return items;
	}

	@Override
	public List<ItemMetadataRMI> getItemsById(Long id) throws RemoteException {
		List<ItemMetadataRMI> list = new ArrayList<ItemMetadataRMI>();

		return list;
	}

	@Override
	public ItemMetadataRMI findById(Long id, Boolean includeList, Long version,
			Boolean includeDeleted, Boolean includeChunks)
			throws RemoteException {
		ItemMetadataRMI item = null;

		return item;
	}

	@Override
	public ItemMetadataRMI findByUserId(UUID userId, Boolean includeDeleted)
			throws RemoteException {
		// TODO: check include_deleted
		ItemMetadataRMI rootMetadata = new ItemMetadataRMI();

		return rootMetadata;
	}

	@Override
	public ItemMetadataRMI findItemVersionsById(Long fileId)
			throws RemoteException {
		// TODO: check include_deleted
		ItemMetadataRMI rootMetadata = new ItemMetadataRMI();

		return rootMetadata;
	}

	@Override
	public List<String> migrateItem(Long itemId, UUID workspaceId)
			throws RemoteException {

		List<String> chunksToMigrate = null;

		return chunksToMigrate;

	}

	private List<String> getChunksToMigrate(Long itemId) throws RemoteException {

		List<String> chunksList = null;

		return chunksList;

	}

}
