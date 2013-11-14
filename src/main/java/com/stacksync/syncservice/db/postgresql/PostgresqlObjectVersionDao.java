package com.stacksync.syncservice.db.postgresql;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.ObjectVersionDAO;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.model.ObjectVersion;

public class PostgresqlObjectVersionDao extends PostgresqlDAO implements ObjectVersionDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlObjectVersionDao.class.getName());

	public PostgresqlObjectVersionDao(Connection connection) {
		super(connection);
	}

	@Override
	public ObjectVersion findByObjectIdAndVersion(Long objectId, Long version) throws DAOException {
		Object[] values = { objectId, version };

		String query = "SELECT ov.*, get_chunks(ov.id) AS chunks FROM object_version ov WHERE ov.object_id = ? and ov.version = ?";

		ResultSet result = null;
		ObjectVersion objectVersion = null;

		try {

			result = executeQuery(query, values);

			if (result.next()) {
				objectVersion = mapObjectVersion(result);
			} else {
				// TODO error, no ha encontrado nada el perroo
				// throw workspace not found??
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return objectVersion;
	}

	@Override
	public void add(ObjectVersion objectVersion) throws DAOException {
		if (!objectVersion.isValid()) {
			throw new IllegalArgumentException("Object version attributes not set");
		}

		Object[] values = { objectVersion.getObject().getId(), objectVersion.getDevice().getId(), objectVersion.getVersion(),
				new java.sql.Timestamp(objectVersion.getServerDateModified().getTime()), objectVersion.getChecksum(),
				new java.sql.Timestamp(objectVersion.getClientDateModified().getTime()), objectVersion.getClientStatus(), objectVersion.getClientFileSize(),
				objectVersion.getClientName(), objectVersion.getClientFilePath() };

		String query = "INSERT INTO object_version( object_id, device_id, version, modified,"
				+ "client_checksum, client_mtime, client_status, client_file_size, client_name, client_path) " + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

		Long id;
		id = executeUpdate(query, values);

		if (id != null) {
			objectVersion.setId(id);
		}

	}

	@Override
	public void update(ObjectVersion objectVersion) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(ObjectVersion objectVersion) throws DAOException {
		// TODO Auto-generated method stub

	}

	private ObjectVersion mapObjectVersion(ResultSet result) throws SQLException {

		ObjectVersion objectVersion = new ObjectVersion();

		objectVersion.setId(result.getLong("id"));
		objectVersion.setVersion(result.getLong("version"));
		objectVersion.setChecksum(result.getLong("client_checksum"));
		objectVersion.setClientDateModified(result.getTimestamp("client_mtime"));
		objectVersion.setServerDateModified(result.getTimestamp("modified"));
		objectVersion.setClientStatus(result.getString("client_status"));
		objectVersion.setClientFileSize(result.getLong("client_file_size"));
		objectVersion.setClientName(result.getString("client_name"));
		objectVersion.setClientFilePath(result.getString("client_path"));

		Object1 object = new Object1();
		object.setId(result.getLong("object_id"));
		objectVersion.setObject(object);

		Device device = new Device();
		device.setId(result.getLong("device_id"));
		objectVersion.setDevice(device);

		Array arrayChunks = result.getArray("chunks");
		String[] strChunks = (String[]) arrayChunks.getArray();

		List<String> chunksList = Arrays.asList(strChunks);
		if (chunksList.contains(null)) {
			// FIXME TODO: In this case the list contains a null parameter.
			// If this happens "parseObjectMetadata" function from
			// JSONReader fails.
			chunksList = new ArrayList<String>();
		}

		List<Chunk> chunks = new ArrayList<Chunk>();
		for (int i = 0; i < chunksList.size(); i++) {
			chunks.add(new Chunk(chunksList.get(i), i + 1));
		}

		objectVersion.setChunks(chunks);

		return objectVersion;
	}

	@Override
	public void insertChunk(Long objectVersionId, Long chunkId, Integer order) throws DAOException {
		Object[] values = { objectVersionId, chunkId, order };

		String query = "INSERT INTO object_version_chunk( object_version_id, chunk_id, chunk_order ) " + "VALUES ( ?, ?, ? )";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void instertChunks(List<Chunk> chunks, long objectVersionId) throws DAOException {
		if (chunks.isEmpty()) {
			throw new IllegalArgumentException("No chunks received");
		}

		List<Object> values = new ArrayList<Object>();

		StringBuilder build = new StringBuilder("INSERT INTO object_version_chunk " + " (object_version_id, client_chunk_name, chunk_order) VALUES ");

		for (int i = 0; i < chunks.size(); i++) {
			build.append("(?, ?, ?)");
			if (i < chunks.size() - 1) {
				build.append(", ");
			} else {
				build.append(";");
			}

			values.add(objectVersionId); // object_version_id
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
	public List<Chunk> findChunks(Long objectVersionId) throws DAOException {
		Object[] values = { objectVersionId };

		String query = "SELECT ovc.* " + " FROM object_version_chunk ovc " + " WHERE ovc.object_version_id=? " + " ORDER BY ovc.chunk_order ASC";

		ResultSet result = null;
		List<Chunk> chunks = new ArrayList<Chunk>();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				Chunk chunk = mapChunk(result);
				chunks.add(chunk);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return chunks;
	}

	private Chunk mapChunk(ResultSet result) throws SQLException {

		Chunk chunk = new Chunk();
		chunk.setOrder(result.getInt("chunk_order"));
		chunk.setClientChunkName(result.getString("client_chunk_name"));

		return chunk;

	}

}
