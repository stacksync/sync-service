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
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.Object1DAO;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.handler.Handler.Status;
import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.model.ObjectVersion;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.ObjectMetadata;

public class PostgresqlObject1DAO extends PostgresqlDAO implements Object1DAO {
	private static final Logger logger = Logger.getLogger(PostgresqlObject1DAO.class.getName());

	public PostgresqlObject1DAO(Connection connection) {
		super(connection);
	}

	@Override
	public Object1 findByPrimaryKey(Long object1ID) throws DAOException {
		ResultSet resultSet = null;
		Object1 object = null;

		String query = "SELECT * FROM object WHERE id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { object1ID });

			if (resultSet.next()) {
				object = mapObject(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return object;
	}

	@Override
	public List<Object1> findAll() throws DAOException {
		ResultSet resultSet = null;
		List<Object1> list = new ArrayList<Object1>();

		String query = "SELECT * FROM CLIENTS";
		try {
			resultSet = executeQuery(query, null);

			while (resultSet.next()) {
				list.add(mapObject(resultSet));
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
		return list;
	}

	@Override
	public void add(Object1 object) throws DAOException {
		if (object.hasParent()) {
			createNewObjectWithParent(object);
		} else {
			createNewObject(object);
		}
	}

	private void createNewObjectWithParent(Object1 object) throws DAOException {

		if (!object.isValid()) {
			throw new IllegalArgumentException("Object attributes not set");
		}

		Object[] values = { object.getWorkspace().getId(), object.getLatestVersion(), object.getParent().getId(), object.getClientFileId(),
				object.getClientFileName(), object.getClientFileMimetype(), object.getClientFolder(), object.getClientParentRootId(),
				object.getClientParentFileId(), object.getClientParentFileVersion(), object.getRootId() };

		String query = "INSERT INTO object ( workspace_id, latest_version, parent_id,"
				+ "client_file_id, client_file_name, client_file_mimetype, client_folder,"
				+ "client_parent_root_id, client_parent_file_id, client_parent_file_version, root_id ) " + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

		Long id;

		id = executeUpdate(query, values);

		if (id != null) {
			object.setId(id);
		}

	}

	private void createNewObject(Object1 object) throws DAOException {

		if (!object.isValid()) {
			throw new IllegalArgumentException("Object attributes not set");
		}

		Object[] values = { object.getRootId(), object.getWorkspace().getId(), object.getLatestVersion(), object.getClientFileId(), object.getClientFileName(),
				object.getClientFileMimetype(), object.getClientFolder(), object.getClientParentRootId(), object.getClientParentFileId(),
				object.getClientParentFileVersion() };

		String query = "INSERT INTO object ( root_id, workspace_id, latest_version, "
				+ "client_file_id, client_file_name, client_file_mimetype, client_folder,"
				+ "client_parent_root_id, client_parent_file_id, client_parent_file_version ) " + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

		Long id;
		id = executeUpdate(query, values);

		if (id != null) {
			object.setId(id);
		}

	}

	@Override
	public void put(Object1 object) throws DAOException {
		if (object.getId() == null) {
			add(object);
		} else {
			update(object);
		}
	}

	@Override
	public void update(Object1 object) throws DAOException {
		if (object.getId() == null || !object.isValid()) {
			throw new IllegalArgumentException("Object attributes not set");
		}

		Long parentId = object.getParent().getId();
		// If id == 0 means parent is null!
		if (parentId == 0) {
			parentId = null;
		}

		Object[] values = { object.getWorkspace().getId(), object.getLatestVersion(), parentId, object.getClientFileId(), object.getClientFileName(),
				object.getClientFileMimetype(), object.getClientFolder(), object.getClientParentRootId(), object.getClientParentFileId(),
				object.getClientParentFileVersion(), object.getId() };

		String query = "UPDATE object SET " + "workspace_id = ?, " + "latest_version = ?, " + "parent_id = ?, " + "client_file_id = ?, "
				+ "client_file_name = ?, " + "client_file_mimetype = ?, " + "client_folder = ?, " + "client_parent_root_id = ?, "
				+ "client_parent_file_id = ?, " + "client_parent_file_version = ? " + "WHERE id = ?";

		executeUpdate(query, values);

	}

	@Override
	public void delete(Long id) throws DAOException {
		// TODO Auto-generated method stub

	}

	private Object1 mapObject(ResultSet result) throws SQLException {

		Object1 object = new Object1();
		object.setId(result.getLong("id"));
		object.setRootId(result.getString("root_id"));
		object.setLatestVersion(result.getLong("latest_version"));
		object.setClientFileId(result.getLong("client_file_id"));
		object.setClientFileName(result.getString("client_file_name"));
		object.setClientFileMimetype(result.getString("client_file_mimetype"));
		object.setClientFolder(result.getBoolean("client_folder"));
		object.setClientParentRootId(result.getString("client_parent_root_id"));
		Long parentFileId = result.getLong("client_parent_file_id");
		if (parentFileId != 0) {
			object.setClientParentFileId(result.getLong("client_parent_file_id"));
			object.setClientParentFileVersion(result.getLong("client_parent_file_version"));
		}

		Workspace w = new Workspace();
		w.setId(result.getLong("workspace_id"));
		object.setWorkspace(w);

		Object1 parent = new Object1();
		parent.setId(result.getLong("parent_id"));
		object.setParent(parent);

		return object;
	}

	@Override
	public Object1 findByClientFileIdAndWorkspace(Long clientFileID, Long workspaceId) throws DAOException {
		Object[] values = { clientFileID, workspaceId };

		String query = "SELECT * FROM object WHERE client_file_id = ? and workspace_id = ?";

		ResultSet result = null;
		Object1 object = null;

		try {
			result = executeQuery(query, values);

			// There is no more than one result due to fileID+workspace is
			// supposed to be UNIQUE
			if (result.next()) {
				object = mapObject(result);
			} else {
				// TODO error, no ha encontrado nada el perroo
				// throw workspace not found??
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return object;
	}

	@Override
	public Object1 findByClientId(long clientID) throws DAOException {
		ResultSet resultSet = null;
		Object1 object = null;

		String query = "SELECT * FROM object WHERE client_file_id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { clientID });

			if (resultSet.next()) {
				object = mapObject(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return object;
	}

	@Override
	public List<Object1> findByWorkspaceId(long workspaceID) throws DAOException {
		Object[] values = { workspaceID };
		String query = "SELECT * FROM object o WHERE o.workspace_id = ?";

		ResultSet result = null;
		List<Object1> objects = new ArrayList<Object1>();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				objects.add(mapObject(result));
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return objects;
	}

	@Override
	public List<Object1> findByWorkspaceName(String workspaceName) throws DAOException {
		Object[] values = { workspaceName };
		String query = "SELECT * FROM object o " + " INNER JOIN workspace w ON o.workspace_id = w.id " + " WHERE w.client_workspace_name = ?";

		ResultSet result = null;
		ArrayList<Object1> objects = new ArrayList<Object1>();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				objects.add(mapObject(result));
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return objects;
	}

	@Override
	public List<ObjectMetadata> getObjectMetadataByWorkspaceName(String workspaceName) throws DAOException {
		Object[] values = { workspaceName };

		String query = "SELECT o.*, ov.*, get_chunks(ov.id) AS chunks " + " FROM workspace w " + " INNER JOIN object o ON w.id = o.workspace_id "
				+ " INNER JOIN object_version ov ON o.id = ov.object_id AND o.latest_version = ov.version " + " WHERE w.client_workspace_name = ? "
				+ " GROUP BY o.id, ov.id";

		ResultSet result = null;
		List<ObjectMetadata> objects = new ArrayList<ObjectMetadata>();
		try {
			result = executeQuery(query, values);

			while (result.next()) {
				ObjectMetadata objectMetadata = mapObjectMetadata(result);
				objects.add(objectMetadata);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return objects;
	}

	@Override
	public List<ObjectMetadata> getObjectsByClientFileId(Long fileId) throws DAOException {
		Object[] values = { fileId };

		String query = "WITH    RECURSIVE " + " q AS  " + " (  " + " SELECT o.id, o.client_file_id, "
				+ "    o.client_parent_file_id, o.root_id, o.client_file_name, ov.version, " + "    o.client_folder, ov.client_file_size, ov.client_status, "
				+ "    o.client_file_mimetype, ov.client_checksum, ov.client_name, "
				+ "    ov.modified, ov.client_mtime, ARRAY[o.id] AS level_array, get_path(o.id) AS path " + " FROM    object o "
				+ " INNER JOIN object_version ov ON o.id = ov.object_id AND o.latest_version = ov.version " + " WHERE   o.client_file_id = ? " + " UNION ALL "
				+ " SELECT o2.id, o2.client_file_id, o2.client_parent_file_id,  " + "    o2.root_id, o2.client_file_name, ov2.version, o2.client_folder, "
				+ "    ov2.client_file_size, ov2.client_status, o2.client_file_mimetype,  "
				+ "    ov2.client_checksum, ov2.client_name, ov2.modified, ov2.client_mtime, "
				+ "    q.level_array || o2.id, q.path || q.client_file_name::TEXT || '/' " + " FROM    q " + " JOIN    object o2 ON o2.parent_id = q.id "
				+ " INNER JOIN object_version ov2 ON o2.id = ov2.object_id AND o2.latest_version = ov2.version " + "	) "
				+ " SELECT  array_upper(level_array, 1) as level, q.* " + " FROM    q " + " ORDER BY  " + "       level_array ASC";

		ResultSet result = null;
		List<ObjectMetadata> list = new ArrayList<ObjectMetadata>();

		try {
			result = executeQuery(query, values);

			if (!resultSetHasRows(result)) {
				throw new DAOException(DAOError.FILE_NOT_FOUND);
			}

			while (result.next()) {
				ObjectMetadata objectMetadata = mapObjectForAPI(result);
				list.add(objectMetadata);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return list;
	}

	@Override
	public ObjectMetadata findByClientFileId(Long fileId, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks) throws DAOException {
		int maxLevel = includeList ? 2 : 1;
		String targetVersion = (version == null) ? "o.latest_version" : version.toString();
		String chunks = (includeChunks) ? ", get_chunks(%s.id) AS chunks" : "";
		// TODO: check include_deleted
		Object[] values = { fileId, maxLevel };

		String query = String.format("WITH    RECURSIVE " + " q AS  " + " (  " + " SELECT o.id, o.client_file_id, "
				+ "    o.client_parent_file_id, o.root_id, o.client_file_name, ov.version, " + "    o.client_folder, ov.client_file_size, ov.client_status, "
				+ "    o.client_file_mimetype, ov.client_checksum, ov.client_name, "
				+ "    ov.modified, ov.client_mtime, ARRAY[o.id] AS level_array, get_path(o.id) AS path " + String.format(chunks, "ov") + " FROM    object o "
				+ " INNER JOIN object_version ov ON o.id = ov.object_id AND %s = ov.version " + " WHERE   o.client_file_id = ? " + " UNION ALL "
				+ " SELECT o2.id, o2.client_file_id, o2.client_parent_file_id,  " + "    o2.root_id, o2.client_file_name, ov2.version, o2.client_folder, "
				+ "    ov2.client_file_size, ov2.client_status, o2.client_file_mimetype,  "
				+ "    ov2.client_checksum, ov2.client_name, ov2.modified, ov2.client_mtime, "
				+ "    q.level_array || o2.id, q.path || q.client_file_name::TEXT || '/' " + String.format(chunks, "ov2") + " FROM    q "
				+ " JOIN    object o2 ON o2.parent_id = q.id " + " INNER JOIN object_version ov2 ON o2.id = ov2.object_id AND o2.latest_version = ov2.version "
				+ " WHERE   array_upper(level_array, 1) < ? " + "	) " + " SELECT  array_upper(level_array, 1) as level, q.* " + " FROM    q " + " ORDER BY  "
				+ "       level_array ASC", targetVersion);

		ResultSet result = null;
		ObjectMetadata object = null;

		try {
			result = executeQuery(query, values);

			if (!resultSetHasRows(result)) {
				throw new DAOException(DAOError.FILE_NOT_FOUND);
			}

			while (result.next()) {
				ObjectMetadata objectMetadata = mapObjectForAPI(result);

				if (objectMetadata.getLevel() == 1) {
					object = objectMetadata;
				} else {
					// object should not be null at this point, but who knows...

					if (object != null && object.getFileId().equals(objectMetadata.getParentFileId())) {
						if (objectMetadata.getStatus().compareTo(Status.DELETED.toString()) == 0) {
							if (includeDeleted) {
								object.addObjectMetadata(objectMetadata);
							}
						} else {
							object.addObjectMetadata(objectMetadata);
						}
					}
				}
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return object;
	}

	@Override
	public ObjectMetadata findByServerUserId(String serverUserId, Boolean includeDeleted) throws DAOException {
		// TODO: check include_deleted
		Object[] values = { serverUserId };

		String query = "WITH RECURSIVE q AS " + " ( " + "    SELECT o.id, o.client_file_id, o.client_parent_file_id, o.root_id, "
				+ "     o.client_file_name, ov.version, o.client_folder, " + "     ov.client_file_size, ov.client_status, o.client_file_mimetype, "
				+ "     ov.client_checksum, ov.client_name, ov.modified, ov.client_mtime, " + "     ARRAY[o.client_file_id] AS level_array, '/' AS path "
				+ "     FROM user1 u  " + "     INNER JOIN workspace_user wu ON u.id = wu.user_id  "
				+ "     INNER JOIN object o ON wu.workspace_id = o.workspace_id  "
				+ "     INNER JOIN object_version ov ON o.id = ov.object_id AND o.latest_version = ov.version  "
				+ "     WHERE u.cloud_id = ? AND o.parent_id IS NULL  " + "     UNION ALL  "
				+ "     SELECT o2.id, o2.client_file_id, o2.client_parent_file_id,   "
				+ "     o2.root_id, o2.client_file_name, ov2.version, o2.client_folder,  "
				+ "     ov2.client_file_size, ov2.client_status, o2.client_file_mimetype,   "
				+ "     ov2.client_checksum, ov2.client_name, ov2.modified, ov2.client_mtime,  "
				+ "     q.level_array || o2.client_file_id, q.path || q.client_file_name::TEXT || '/'  " + "     FROM q  "
				+ "     JOIN object o2 ON o2.parent_id = q.id  "
				+ "     INNER JOIN object_version ov2 ON o2.id = ov2.object_id AND o2.latest_version = ov2.version  "
				+ "     WHERE array_upper(level_array, 1) < 1 " + " )  " + " SELECT array_upper(level_array, 1) as level, q.*  " + " FROM q  "
				+ " ORDER BY level_array ASC";

		ResultSet result = null;

		// create the virtual ObjectMetadata for the root folder
		ObjectMetadata rootMetadata = new ObjectMetadata();
		rootMetadata.setFolder(true);
		rootMetadata.setFileName("root");
		rootMetadata.setIsRoot(true);

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				ObjectMetadata objectMetadata = mapObjectForAPI(result);

				if (objectMetadata.getStatus().compareTo(Status.DELETED.toString()) == 0) {
					if (includeDeleted) {
						rootMetadata.addObjectMetadata(objectMetadata);
					}
				} else {
					rootMetadata.addObjectMetadata(objectMetadata);
				}
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return rootMetadata;
	}

	@Override
	public ObjectMetadata findObjectVersionsByClientFileId(Long fileId) throws DAOException {
		// TODO: check include_deleted
		Object[] values = { fileId };

		String query = "SELECT o.*, ov.version, ov.client_file_size, ov.client_status, ov.client_name, ov.client_checksum, "
				+ " ov.modified, ov.client_mtime, '1' AS level, '' AS path FROM object o "
				+ " inner join object_version ov on ov.object_id = o.id  where o.client_file_id = ? order by ov.version DESC ";

		ResultSet result = null;

		// create the virtual ObjectMetadata for the root folder
		ObjectMetadata rootMetadata = new ObjectMetadata();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				ObjectMetadata objectMetadata = mapObjectForAPI(result);

				if (rootMetadata.getContent().isEmpty()) {
					rootMetadata = objectMetadata;
				}
				rootMetadata.addObjectMetadata(objectMetadata);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return rootMetadata;
	}

	private boolean resultSetHasRows(ResultSet resultSet) {
		boolean hasRows = false;
		if (resultSet != null) {
			try {
				// true if the cursor is before the first row; false if the
				// cursor is at any other position or the result set contains no
				// rows }
				hasRows = resultSet.isBeforeFirst();
			} catch (SQLException e) {
			}
		}
		return hasRows;
	}

	private ObjectMetadata mapObjectForAPI(ResultSet result) throws SQLException {

		ObjectMetadata metadata = new ObjectMetadata();

		metadata.setFileId(DAOUtil.getLongFromResultSet(result, "client_file_id"));
		metadata.setParentFileId(DAOUtil.getLongFromResultSet(result, "client_parent_file_id"));
		metadata.setRootId(result.getString("root_id"));
		metadata.setFileName(result.getString("client_file_name"));
		metadata.setVersion(result.getLong("version"));
		metadata.setFolder(result.getBoolean("client_folder"));
		metadata.setFileSize(result.getLong("client_file_size"));
		metadata.setStatus(result.getString("client_status"));
		metadata.setMimetype(result.getString("client_file_mimetype"));
		metadata.setChecksum(result.getLong("client_checksum"));
		metadata.setClientName(result.getString("client_name"));
		metadata.setServerDateModified(result.getTimestamp("modified"));
		metadata.setClientDateModified(result.getTimestamp("client_mtime"));
		metadata.setFilePath(result.getString("path"));
		metadata.setLevel(result.getInt("level"));

		try {
			Array arrayChunks = result.getArray("chunks");
			String[] chunks = (String[]) arrayChunks.getArray();

			List<String> chunksList = Arrays.asList(chunks);
			if (chunksList.contains(null)) {
				// FIXME TODO: In this case the list contains a null parameter.
				// If this happens "parseObjectMetadata" function from
				// JSONReader fails.
				chunksList = new ArrayList<String>();
			}
			metadata.setChunks(chunksList);
		} catch (Exception e) {
			metadata.setChunks(new ArrayList<String>());
		}

		return metadata;
	}

	private ObjectMetadata mapObjectMetadata(ResultSet result) throws SQLException {

		Object1 object = mapObject(result);
		ObjectVersion objectVersion = mapObjectVersion(result);

		ObjectMetadata metadata = new ObjectMetadata(object.getRootId(), object.getClientFileId(), objectVersion.getVersion(), object.getClientParentRootId(),
				object.getClientParentFileId(), object.getClientParentFileVersion(), objectVersion.getServerDateModified(), objectVersion.getClientStatus(),
				objectVersion.getClientDateModified(), objectVersion.getChecksum(), objectVersion.getClientName(), null, objectVersion.getClientFileSize(),
				object.getClientFolder(), object.getClientFileName(), objectVersion.getClientFilePath(), object.getClientFileMimetype());

		if (!object.getClientFolder()) {
			Array arrayChunks = result.getArray("chunks");
			String[] chunks = (String[]) arrayChunks.getArray();

			List<String> chunksList = Arrays.asList(chunks);
			if (chunksList.contains(null)) {
				// FIXME TODO: In this case the list contains a null parameter.
				// If this happens "parseObjectMetadata" function from
				// JSONReader fails.
				chunksList = new ArrayList<String>();
			}
			metadata.setChunks(chunksList);
		}

		return metadata;
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

}
