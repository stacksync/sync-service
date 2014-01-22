package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.ItemVersion;
import com.stacksync.syncservice.models.ItemMetadata;

public interface ItemVersionDAO {

	public ItemMetadata findByItemIdAndVersion(Long id, Long version) throws DAOException;;

	public void add(ItemVersion itemVersion) throws DAOException;

	public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws DAOException;

	public void insertChunks(List<Chunk> chunks, long itemVersionId) throws DAOException;

	public List<Chunk> findChunks(Long itemVersionId) throws DAOException;

	public void update(ItemVersion itemVersion) throws DAOException;

	public void delete(ItemVersion itemVersion) throws DAOException;
}
