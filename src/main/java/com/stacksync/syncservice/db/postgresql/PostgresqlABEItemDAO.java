/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.postgresql;

import com.stacksync.commons.models.ABEItemMetadata;
import com.stacksync.syncservice.db.ABEItemDAO;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * @author javigd
 */
public class PostgresqlABEItemDAO extends PostgresqlItemDAO implements ABEItemDAO {

    	private static final Logger logger = Logger
			.getLogger(PostgresqlABEItemDAO.class.getName());
    
        public PostgresqlABEItemDAO(Connection connection) {
            super(connection);
        }

        @Override
        public List<ABEItemMetadata> getABEItemsByWorkspaceId(UUID workspaceId) throws DAOException {
            //TODO: elaborate the SQL query within
            Object[] values = { workspaceId, workspaceId };

            String query = "WITH RECURSIVE q AS "
                            + "( "
                            + " SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, "
                            + " i.filename, iv.id AS version_id, iv.version, i.is_folder, i.encrypted_dek, "
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
                            + " SELECT array_upper(level_array, 1) as level, "
                            + " c.id as abemeta_id, c.item_id, c.attribute, c.encrypted_pk_component, c.version, "
                            + " q.*, get_chunks(q.version_id) AS chunks, "
                            + " FROM q  "
                            + " LEFT OUTER JOIN abe_component c "
                            + " ON c.item_id = q.item_id "
                            + " ORDER BY level_array ASC";

            ResultSet result = null;
            Map<Long, ABEItemMetadata> itemMap;
            List<ABEItemMetadata> items;
            
            try {
                    result = executeQuery(query, values);
                    
                    itemMap = new HashMap<Long, ABEItemMetadata>();

                    while (result.next()) {
                            ABEItemMetadata item = DAOUtil
                                            .getABEItemMetadataFromResultSet(result);
                            if (!itemMap.containsKey(item.getId())) {
                                itemMap.put(item.getId(), item);
                            } else {
                                ABEItemMetadata it = itemMap.get(item.getId());
                                it.getAbeComponents().addAll(item.getAbeComponents());
                                itemMap.replace(item.getId(), it);
                            }
                    }

            } catch (SQLException e) {
                    logger.error(e);
                    throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
            }
            items = new ArrayList<ABEItemMetadata>(itemMap.values());
            return items;
        }
}
