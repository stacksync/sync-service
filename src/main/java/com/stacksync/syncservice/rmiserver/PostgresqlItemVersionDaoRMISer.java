package com.stacksync.syncservice.rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
//import com.stacksync.syncservice.db.DAOError;
//import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.rmiserveri.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class PostgresqlItemVersionDaoRMISer extends UnicastRemoteObject implements
		ItemVersionDAORMISer {

	// private static final Logger logger =
	// Logger.getLogger(PostgresqlItemVersionDao.class.getName());

	public PostgresqlItemVersionDaoRMISer() throws RemoteException {
		super();
	}

	@Override
	public ItemMetadata findByItemIdAndVersion(Long id, Long version)
			throws DAOException {

		ItemMetadata metadata = null;

		return metadata;
	}

	@Override
	public void add(ItemVersion itemVersion) throws DAOException {

	}

	@Override
	public void update(ItemVersion itemVersion) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(ItemVersion itemVersion) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void insertChunk(Long itemVersionId, Long chunkId, Integer order)
			throws DAOException {

	}

	@Override
	public void insertChunks(List<Chunk> chunks, long itemVersionId)
			throws DAOException {

	}

	@Override
	public List<Chunk> findChunks(Long itemVersionId) throws DAOException {
		List<Chunk> chunks = new ArrayList<Chunk>();

		return chunks;
	}

}
