package com.stacksync.syncservice.db;

import java.util.List;
import java.util.UUID;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface ItemVersionDAO {

	public ItemMetadata findByItemIdAndVersion(UUID userID, Long id, Long version) throws DAOException;;

	public void add(User user, ItemVersion itemVersion) throws DAOException;

	public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws DAOException;

	public void insertChunks(User user, List<Chunk> chunks, long itemVersionId) throws DAOException;

	public List<Chunk> findChunks(UUID userID, Long itemVersionId) throws DAOException;

	public void update(ItemVersion itemVersion) throws DAOException;

	public void delete(ItemVersion itemVersion) throws DAOException;
}
