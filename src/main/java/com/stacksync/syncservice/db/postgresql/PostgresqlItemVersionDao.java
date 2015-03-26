package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class PostgresqlItemVersionDao extends PostgresqlDAO implements ItemVersionDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlItemVersionDao.class.getName());

	public PostgresqlItemVersionDao(Connection connection) {
		super(connection);
	}

	@Override
	public ItemMetadata findByItemIdAndVersion(UUID userID, Long id, Long version) throws DAOException {
		Object[] values = { userID, id, version };

		String query = "SELECT * FROM find_by_item_id_and_version(?::uuid, ?, ?)";

		ResultSet result = null;
		ItemMetadata metadata = null;

		try {

			result = executeQuery(query, values);

			if (result.next()) {

				metadata = DAOUtil.getItemMetadataFromResultSet(result);
			} else {
				// TODO error, no ha encontrado nada el perroo -> wtf?
				// throw workspace not found??
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return metadata;
	}

	@Override
	public void add(User user, ItemVersion itemVersion) throws DAOException {
		if (!itemVersion.isValid()) {
			throw new IllegalArgumentException("Item version attributes not set");
		}

		Object[] values = { user.getId(), itemVersion.getItem().getId(), itemVersion.getDevice().getId(), itemVersion.getVersion(),
				itemVersion.getChecksum(), itemVersion.getStatus(), itemVersion.getSize(),
				new java.sql.Timestamp(itemVersion.getModifiedAt().getTime()) };

		String query = "SELECT add_item_version(?::uuid, ?, ?, ?, ?, ?, ?, ?)";

		ResultSet resultSet = executeQuery(query, values);

		Long id;

		try {
			if (resultSet.next()) {
				id = (Long) resultSet.getObject(1);
				if (id != null) {
					itemVersion.setId(id);
				}
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(e);
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

		String query = "INSERT INTO item_version_chunk( item_version_id, chunk_id, chunk_order ) " + "VALUES ( ?, ?, ? )";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
	}

	// @Override
	// public void insertChunks(List<Chunk> chunks, long itemVersionId) throws
	// DAOException {
	// if (chunks.isEmpty()) {
	// throw new IllegalArgumentException("No chunks received");
	// }
	//
	// List<Object> values = new ArrayList<Object>();
	//
	// StringBuilder build = new StringBuilder("INSERT INTO item_version_chunk "
	// + " (item_version_id, client_chunk_name, chunk_order) VALUES ");
	//
	// for (int i = 0; i < chunks.size(); i++) {
	// build.append("(?, ?, ?)");
	// if (i < chunks.size() - 1) {
	// build.append(", ");
	// } else {
	// build.append(";");
	// }
	//
	// values.add(itemVersionId); // item_version_id
	// values.add(chunks.get(i).getClientChunkName()); // client_chunk_name
	// values.add(i + 1); // chunk_order
	// }
	//
	// try {
	// executeUpdate(build.toString(), values.toArray());
	//
	// } catch (DAOException ex) {
	// throw new DAOException(ex);
	// }
	// }

	@Override
	public void insertChunks(User user, List<Chunk> chunks, long itemVersionId) throws DAOException {
		if (chunks.isEmpty()) {
			throw new IllegalArgumentException("No chunks received");
		}

		String str = "INSERT INTO item_version_chunk (item_version_id, client_chunk_name, chunk_order) VALUES ";

		for (int i = 0; i < chunks.size(); i++) {
			String chunkName = chunks.get(i).getClientChunkName();
			str += "(" + itemVersionId + ", '" + chunkName + "', " + (i + 1) + ")";

			if (i < chunks.size() - 1) {
				str += ", ";
			}
			// else {
			// str += ";";
			// }
		}

		Object[] values = { user.getId(), str };
		
		String query = "SELECT * FROM dynamic_query(?::uuid, ?)";
		
		try {
			executeQuery(query, values);
		} catch (DAOException ex) {
			throw new DAOException(ex);
		}
	}

	@Override
	public List<Chunk> findChunks(UUID userID, Long itemVersionId) throws DAOException {
		Object[] values = { userID, itemVersionId };

		String query = "SELECT * FROM find_chunks(?::uuid, ?)";

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
