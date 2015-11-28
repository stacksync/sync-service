package com.stacksync.syncservice.db;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;

import java.util.List;

public interface ItemVersionDAO {

	ItemMetadataRMI findByItemIdAndVersion(Long id, Long version) throws DAOException;;

	void add(ItemVersion itemVersion) throws DAOException;

	void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws DAOException;

	void insertChunks(Chunk[] chunks, long itemVersionId) throws DAOException;

	List<Chunk> findChunks(Long itemVersionId) throws DAOException;

	void update(ItemVersion itemVersion) throws DAOException;

	void delete(ItemVersion itemVersion) throws DAOException;
}
