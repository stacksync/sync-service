package com.stacksync.syncservice.db.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class PostgresqlItemVersionDao extends PostgresqlDAO implements ItemVersionDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlItemVersionDao.class.getName());

	public PostgresqlItemVersionDao(Connection connection) {
		super((PostgresqlConnection)connection);
	}

	@Override
	public ItemMetadata findByItemIdAndVersion(Long id, Long version) throws DAOException {
		Object[] values = { id, version };

		String query = "SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, "
				+ " iv.version, iv.device_id, iv.checksum, iv.status, iv.size, iv.modified_at, "
				+ " get_chunks(iv.id) AS chunks "
				+ " FROM item_version iv "
				+ " INNER JOIN item i ON i.id = iv.item_id " 
				+ " WHERE iv.item_id = ? and iv.version = ?";

		ResultSet result = null;
		ItemMetadata metadata = null;

		try {

			result = executeQuery(query, values);

			if (result.next()) {

				metadata = DAOUtil.getItemMetadataFromResultSet(result);
			} else {
				// TODO error, no ha encontrado nada el perroo
				// throw workspace not found??
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return metadata;
	}

	@Override
	public void add(ItemVersion itemVersion) throws DAOException {
		if (!itemVersion.isValid()) {
			throw new IllegalArgumentException("Item version attributes not set");
		}

		Object[] values = { itemVersion.getItem().getId(), itemVersion.getDevice().getId(), itemVersion.getVersion(),
				itemVersion.getChecksum(), itemVersion.getStatus(), itemVersion.getSize(),
				new java.sql.Timestamp(itemVersion.getModifiedAt().getTime()) };

		String query = "INSERT INTO item_version( item_id, device_id, version, "
				+ "checksum, status, size, modified_at, committed_at ) " + "VALUES ( ?, ?, ?, ?, ?, ?, ?, now() )";

		Long id = (Long)executeUpdate(query, values);

		if (id != null) {
			itemVersion.setId(id);
		}

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
	public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws DAOException {
		Object[] values = { itemVersionId, chunkId, order };

		String query = "INSERT INTO item_version_chunk( item_version_id, chunk_id, chunk_order ) "
				+ "VALUES ( ?, ?, ? )";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void insertChunks(List<Chunk> chunks, long itemVersionId) throws DAOException {
		if (chunks.isEmpty()) {
			throw new IllegalArgumentException("No chunks received");
		}

		List<Object> values = new ArrayList<Object>();

		StringBuilder build = new StringBuilder("INSERT INTO item_version_chunk "
				+ " (item_version_id, client_chunk_name, chunk_order) VALUES ");

		for (int i = 0; i < chunks.size(); i++) {
			build.append("(?, ?, ?)");
			if (i < chunks.size() - 1) {
				build.append(", ");
			} else {
				build.append(";");
			}

			values.add(itemVersionId); // item_version_id
			values.add(chunks.get(i).getClientChunkName()); // client_chunk_name
			values.add(i + 1); // chunk_order
		}

		try {
			executeUpdate(build.toString(), values.toArray());

		} catch (DAOException ex) {
			throw new DAOException(ex);
		}
	}

	@Override
	public List<Chunk> findChunks(Long itemVersionId) throws DAOException {
		Object[] values = { itemVersionId };

		String query = "SELECT ivc.* " + " FROM item_version_chunk ivc " + " WHERE ivc.item_version_id=? "
				+ " ORDER BY ivc.chunk_order ASC";

		ResultSet result = null;
		List<Chunk> chunks = new ArrayList<Chunk>();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				Chunk chunk = DAOUtil.getChunkFromResultSet(result);
				chunks.add(chunk);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return chunks;
	}

}
