package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
//import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//import org.apache.log4j.Logger;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
//import com.stacksync.syncservice.db.DAOError;
//import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;

//import com.stacksync.syncservice.handler.Handler.Status;

public class ItemDAORMISer extends UnicastRemoteObject implements ItemDAORMIIfc {
	// private static final Logger logger = Logger
	// .getLogger(PostgresqlItemDAO.class.getName());

	public ItemDAORMISer() throws RemoteException {
		super();
	}

	@Override
	public Item findById(Long item1ID) throws DAOException {
		Item item = null;

		return item;
	}

	@Override
	public void add(Item item) throws DAOException {

	}

	@Override
	public void put(Item item) throws DAOException {

	}

	@Override
	public void update(Item item) throws DAOException {

	}

	@Override
	public void delete(Long id) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ItemMetadata> getItemsByWorkspaceId(UUID workspaceId)
			throws DAOException {

		List<ItemMetadata> items = null;

		return items;
	}

	@Override
	public List<ItemMetadata> getItemsById(Long id) throws DAOException {
		List<ItemMetadata> list = new ArrayList<ItemMetadata>();

		return list;
	}

	@Override
	public ItemMetadata findById(Long id, Boolean includeList, Long version,
			Boolean includeDeleted, Boolean includeChunks) throws DAOException {
		ItemMetadata item = null;

		return item;
	}

	@Override
	public ItemMetadata findByUserId(UUID userId, Boolean includeDeleted)
			throws DAOException {
		// TODO: check include_deleted
		ItemMetadata rootMetadata = new ItemMetadata();

		return rootMetadata;
	}

	@Override
	public ItemMetadata findItemVersionsById(Long fileId) throws DAOException {
		// TODO: check include_deleted
		ItemMetadata rootMetadata = new ItemMetadata();

		return rootMetadata;
	}

	private boolean resultSetHasRows(ResultSet resultSet) {
		boolean hasRows = false;

		return hasRows;
	}

	@Override
	public List<String> migrateItem(Long itemId, UUID workspaceId)
			throws DAOException {

		List<String> chunksToMigrate = null;

		return chunksToMigrate;

	}

	private List<String> getChunksToMigrate(Long itemId) throws DAOException,
			SQLException {

		List<String> chunksList = null;

		return chunksList;

	}

}
