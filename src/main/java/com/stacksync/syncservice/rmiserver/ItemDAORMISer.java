package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.rmiclient.*;
import com.stacksync.syncservice.rmiserveri.*;

public class ItemDAORMISer extends UnicastRemoteObject implements ItemDAORMIIfc {

	public ItemDAORMISer() throws RemoteException {
		super();
	}

	@Override
	public ItemRMI findById(Long item1ID) throws RemoteException {
		ItemRMI item = null;

		return item;
	}

	@Override
	public void add(ItemRMI item) throws RemoteException {

	}

	@Override
	public void put(ItemRMI item) throws RemoteException {

	}

	@Override
	public void update(ItemRMI item) throws RemoteException {

	}

	@Override
	public void delete(Long id) throws RemoteException {
		// TODO Auto-generated method stub

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
			Boolean includeDeleted, Boolean includeChunks) throws RemoteException {
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
	public ItemMetadataRMI findItemVersionsById(Long fileId) throws RemoteException {
		// TODO: check include_deleted
		ItemMetadataRMI rootMetadata = new ItemMetadataRMI();

		return rootMetadata;
	}

	private boolean resultSetHasRows(ResultSet resultSet) {
		boolean hasRows = false;

		return hasRows;
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
