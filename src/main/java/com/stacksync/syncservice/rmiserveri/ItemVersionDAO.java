package com.stacksync.syncservice.rmiserveri;

import java.util.List;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface ItemVersionDAO {

	public ItemMetadata findByItemIdAndVersion(Long id, Long version) throws DAOException;;

	public void add(ItemVersion itemVersion) throws DAOException;

	public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws DAOException;

	public void insertChunks(List<Chunk> chunks, long itemVersionId) throws DAOException;

	public List<Chunk> findChunks(Long itemVersionId) throws DAOException;

	public void update(ItemVersion itemVersion) throws DAOException;

	public void delete(ItemVersion itemVersion) throws DAOException;
}
