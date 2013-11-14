package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.ObjectVersion;

public interface ObjectVersionDAO {

	public ObjectVersion findByObjectIdAndVersion(Long objectId, Long version) throws DAOException;;

	public void add(ObjectVersion objectVersion) throws DAOException;

	public void insertChunk(Long objectVersionId, Long chunkId, Integer order) throws DAOException;

	public void instertChunks(List<Chunk> chunks, long objectVersionId) throws DAOException;

	public List<Chunk> findChunks(Long objectVersionId) throws DAOException;

	public void update(ObjectVersion objectVersion) throws DAOException;

	public void delete(ObjectVersion objectVersion) throws DAOException;
}
