package com.stacksync.syncservice.db;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;

import java.util.List;

public interface ItemVersionDAO {

	public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version) throws DAOException;;

	public void add(ItemVersion itemVersion) throws DAOException;

	public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws DAOException;

	public void insertChunks(List<Chunk> chunks, long itemVersionId) throws DAOException;

	public List<Chunk> findChunks(Long itemVersionId) throws DAOException;

	public void update(ItemVersion itemVersion) throws DAOException;

	public void delete(ItemVersion itemVersion) throws DAOException;
}
