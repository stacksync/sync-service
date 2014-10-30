package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler.Status;

public class PostgresqlItemDAO extends PostgresqlDAO implements ItemDAO {
	private static final Logger logger = Logger
			.getLogger(PostgresqlItemDAO.class.getName());

	public PostgresqlItemDAO(Connection connection) {
		super(connection);
	}

	@Override
	public Item findById(Long item1ID) throws DAOException {
		ResultSet resultSet = null;
		Item item = null;

		String query = "SELECT * FROM item WHERE id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { item1ID });

			if (resultSet.next()) {
				item = DAOUtil.getItemFromResultSet(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return item;
	}

	@Override
	public void add(Item item) throws DAOException {

		if (!item.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}

		Object[] values = { item.getWorkspace().getId(),
				item.getLatestVersion(), item.getParentId(),
				item.getFilename(), item.getMimetype(), item.isFolder(),
				item.getClientParentFileVersion() };

		String query = "INSERT INTO item ( workspace_id, latest_version, parent_id,"
				+ " filename, mimetype, is_folder,"
				+ " client_parent_file_version ) "
				+ "VALUES ( ?::uuid, ?, ?, ?, ?, ?, ? )";

		Long id = (Long)executeUpdate(query, values);

		if (id != null) {
			item.setId(id);
		}

	}

	@Override
	public void put(Item item) throws DAOException {
		if (item.getId() == null) {
			add(item);
		} else {
			update(item);
		}
	}

	@Override
	public void update(Item item) throws DAOException {
		if (item.getId() == null || !item.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}

		Long parentId = item.getParentId();
		// If id == 0 means parent is null!
		if (parentId != null && parentId == 0) {
			parentId = null;
		}

		Object[] values = { item.getWorkspace().getId(),
				item.getLatestVersion(), parentId, item.getFilename(),
				item.getMimetype(), item.isFolder(),
				item.getClientParentFileVersion(), item.getId() };

		String query = "UPDATE item SET " + "workspace_id = ?::uuid, "
				+ "latest_version = ?, " + "parent_id = ?, " + "filename = ?, "
				+ "mimetype = ?, " + "is_folder = ?, "
				+ "client_parent_file_version = ? " + "WHERE id = ?";

		executeUpdate(query, values);

	}

	@Override
	public void delete(Long id) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ItemMetadata> getItemsByWorkspaceId(UUID workspaceId)
			throws DAOException {

		Object[] values = { workspaceId, workspaceId };

		String query = "WITH RECURSIVE q AS "
				+ "( "
				+ " SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, "
				+ " i.filename, iv.id AS version_id, iv.version, i.is_folder, "
				+ " i.workspace_id, "
				+ " iv.size, iv.status, i.mimetype, "
				+ " iv.checksum, iv.device_id, iv.modified_at, "
				+ " ARRAY[i.id] AS level_array "
				+ " FROM workspace w  "
				+ " INNER JOIN item i ON w.id = i.workspace_id "
				+ " INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version "
				+ " WHERE w.id = ?::uuid AND i.parent_id IS NULL "
				+ " UNION ALL  "
				+ " SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, "
				+ " i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder,  "
				+ " i2.workspace_id, "
				+ " iv2.size, iv2.status, i2.mimetype, "
				+ " iv2.checksum, iv2.device_id, iv2.modified_at,  "
				+ " q.level_array || i2.id "
				+ " FROM q  "
				+ " JOIN item i2 ON i2.parent_id = q.item_id "
				+ " INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version "
				+ " WHERE i2.workspace_id=?::uuid "
				+ " )  "
				+ " SELECT array_upper(level_array, 1) as level, q.*, get_chunks(q.version_id) AS chunks "
				+ " FROM q  " 
				+ " ORDER BY level_array ASC";

		ResultSet result = null;
		List<ItemMetadata> items;
		try {
			result = executeQuery(query, values);

			items = new ArrayList<ItemMetadata>();

			while (result.next()) {
				ItemMetadata item = DAOUtil
						.getItemMetadataFromResultSet(result);
				items.add(item);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return items;
	}

	@Override
	public List<ItemMetadata> getItemsById(Long id) throws DAOException {
		Object[] values = { id };
		
		String query = "WITH    RECURSIVE "
				+ " q AS  "
				+ " (  "
				+ " SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, "
				+ " 	i.filename, iv.id AS version_id, iv.version, i.is_folder, "
				+ " 	i.workspace_id, "
				+ " 	iv.size, iv.status, i.mimetype, "
				+ " 	iv.checksum, iv.device_id, iv.modified_at, "
				+ " 	ARRAY[i.id] AS level_array "
				+ " FROM    item i "
				+ " INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version "
				+ " WHERE   i.id = ? "
				+ " UNION ALL "
				+ " SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, "
				+ " 	i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder,  "
				+ " 	i2.workspace_id, "
				+ " 	iv2.size, iv2.status, i2.mimetype, "
				+ " 	iv2.checksum, iv2.device_id, iv2.modified_at,  "
				+ " 	q.level_array || i2.id "
				+ " FROM    q "
				+ " JOIN    item i2 ON i2.parent_id = q.item_id "
				+ " INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version "
				+ "	) " 
				+ " SELECT  array_upper(level_array, 1) as level, q.* "
				+ " FROM    q " 
				+ " ORDER BY  " 
				+ "       level_array ASC";

		ResultSet result = null;
		List<ItemMetadata> list = new ArrayList<ItemMetadata>();

		try {
			result = executeQuery(query, values);

			if (!resultSetHasRows(result)) {
				throw new DAOException(DAOError.FILE_NOT_FOUND);
			}

			while (result.next()) {
				ItemMetadata itemMetadata = DAOUtil
						.getItemMetadataFromResultSet(result);
				list.add(itemMetadata);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return list;
	}

	@Override
	public ItemMetadata findById(Long id, Boolean includeList,
			Long version, Boolean includeDeleted, Boolean includeChunks)
			throws DAOException {
		int maxLevel = includeList ? 2 : 1;
		String targetVersion = (version == null) ? "i.latest_version" : version
				.toString();
		String chunks = (includeChunks) ? ", get_chunks(%s.id) AS chunks" : "";
		// TODO: check include_deleted
		Object[] values = { id, maxLevel };

		String query = String
				.format("WITH    RECURSIVE "
						+ " q AS  "
						+ " (  "
						+ " SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, " 
						+ "     i.filename, iv.version, i.is_folder, "
						+ "     iv.device_id, i.workspace_id, iv.size, iv.status, i.mimetype, "
						+ "     iv.checksum, iv.modified_at, "
						+ "     ARRAY[i.id] AS level_array "
						+ String.format(chunks, "iv")
						+ " FROM    item i "
						+ " INNER JOIN item_version iv ON i.id = iv.item_id AND %s = iv.version "
						+ " WHERE   i.id = ? "
						+ " UNION ALL "
						+ " SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, "
						+ "     i2.filename, iv2.version, i2.is_folder, "
						+ "     iv2.device_id, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, "
						+ "     iv2.checksum, iv2.modified_at, "
						+ "     q.level_array || i2.id "
						+ String.format(chunks, "iv2")
						+ " FROM    q "
						+ " JOIN    item i2 ON i2.parent_id = q.item_id "
						+ " INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version "
						+ " WHERE   array_upper(level_array, 1) < ? " + "	) "
						+ " SELECT  array_upper(level_array, 1) as level, q.* "
						+ " FROM    q " + " ORDER BY  "
						+ "       level_array ASC", targetVersion);

		ResultSet result = null;
		ItemMetadata item = null;

		try {
			result = executeQuery(query, values);

			if (!resultSetHasRows(result)) {
				throw new DAOException(DAOError.FILE_NOT_FOUND);
			}

			while (result.next()) {
				ItemMetadata itemMetadata = DAOUtil
						.getItemMetadataFromResultSet(result);

				if (itemMetadata.getLevel() == 1) {
					item = itemMetadata;
				} else {
					// item should not be null at this point, but who knows...

					if (item != null
							&& item.getId().equals(itemMetadata.getParentId())) {
						if (itemMetadata.getStatus().compareTo(
								Status.DELETED.toString()) == 0) {
							if (includeDeleted) {
								item.addChild(itemMetadata);
							}
						} else {
							item.addChild(itemMetadata);
						}
					}
				}
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return item;
	}

	@Override
	public ItemMetadata findByUserId(UUID userId,
			Boolean includeDeleted) throws DAOException {
		// TODO: check include_deleted
		Object[] values = { userId };

		String query = "WITH RECURSIVE q AS "
				+ " ( "
				+ "    SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, "
				+ "     i.filename, iv.device_id, i.workspace_id, iv.version, i.is_folder, "
				+ "     iv.size, iv.status, i.mimetype, "
				+ "     iv.checksum, iv.modified_at, "
				+ "     ARRAY[i.id] AS level_array, '/' AS path "
				+ "     FROM user1 u  "
				+ "     INNER JOIN workspace_user wu ON u.id = wu.user_id  "
				+ "     INNER JOIN item i ON wu.workspace_id = i.workspace_id  "
				+ "     INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version  "
				+ "     WHERE u.id = ?::uuid AND i.parent_id IS NULL  "
				+ "     UNION ALL  "
				+ "     SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version,  "
				+ "     i2.filename, iv2.device_id, i2.workspace_id, iv2.version, i2.is_folder,  "
				+ "     iv2.size, iv2.status, i2.mimetype,   "
				+ "     iv2.checksum, iv2.modified_at,  "
				+ "     q.level_array || i2.id, q.path || q.filename::TEXT || '/'  "
				+ "     FROM q  "
				+ "     JOIN item i2 ON i2.parent_id = q.item_id  "
				+ "     INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version  "
				+ "     WHERE array_upper(level_array, 1) < 1 " + " )  "
				+ " SELECT array_upper(level_array, 1) as level, q.*  "
				+ " FROM q  " + " ORDER BY level_array ASC";

		ResultSet result = null;

		// create the virtual ItemMetadata for the root folder
		ItemMetadata rootMetadata = new ItemMetadata();
		rootMetadata.setIsFolder(true);
		rootMetadata.setFilename("root");
		rootMetadata.setIsRoot(true);

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				ItemMetadata itemMetadata = DAOUtil
						.getItemMetadataFromResultSet(result);

				if (itemMetadata.getStatus().compareTo(
						Status.DELETED.toString()) == 0) {
					if (includeDeleted) {
						rootMetadata.addChild(itemMetadata);
					}
				} else {
					rootMetadata.addChild(itemMetadata);
				}
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return rootMetadata;
	}

	@Override
	public ItemMetadata findItemVersionsById(Long fileId) throws DAOException {
		// TODO: check include_deleted
		Object[] values = { fileId };
		
		String query = "SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, "
				+ " iv.version, iv.size, iv.status, iv.checksum, iv.device_id, "
				+ " iv.modified_at, '1' AS level, '' AS path FROM item i "
				+ " inner join item_version iv on iv.item_id = i.id  where i.id = ? ORDER BY iv.version DESC ";

		ResultSet result = null;

		// create the virtual ItemMetadata for the root folder
		ItemMetadata rootMetadata = new ItemMetadata();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				ItemMetadata itemMetadata = DAOUtil
						.getItemMetadataFromResultSet(result);

				if (rootMetadata.getChildren().isEmpty()) {
					rootMetadata = itemMetadata;
				}
				rootMetadata.addChild(itemMetadata);
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
	
	@Override
	public List<String> migrateItem(Long itemId, UUID workspaceId) throws DAOException{
		
		Object[] values = { itemId, workspaceId.toString() };
		
		String query = "WITH    RECURSIVE "
			+ " q AS "  
			+ " ( "
			+ " SELECT i.* "
			+ " FROM    item i "
			+ " WHERE   i.id = ? " 
			+ " UNION ALL "
			+ " SELECT i2.* "
			+ " FROM    q "
			+ " JOIN    item i2 ON i2.parent_id = q.id " 
			+ " ) "
			+ " UPDATE item i3 SET workspace_id = ?::uuid "
			+ " FROM q "
			+ " WHERE q.id = i3.id";
		
		executeUpdate(query, values);
		
		List<String> chunksToMigrate;
		
		try{
			chunksToMigrate = getChunksToMigrate(itemId);
		}catch (SQLException e){
			throw new DAOException(e);
		}
		
		return chunksToMigrate;
		
	}
	
	private List<String> getChunksToMigrate(Long itemId) throws DAOException, SQLException {
		
		Object[] values = { itemId };
		
		String query = "SELECT get_unique_chunks_to_migrate(?) AS chunks";
		
		ResultSet result = executeQuery(query, values);
		List<String> chunksList;
		
		if (result.next()){
			chunksList = DAOUtil.getArrayFromResultSet(result, "chunks");
		}
		else{
			chunksList = new ArrayList<String>();
		}
	
		
		return chunksList;
		
	}

}
