package com.stacksync.syncservice.db.postgresql;

import com.stacksync.commons.models.abe.ABEItem;
import com.stacksync.commons.models.abe.ABEItemMetadata;
import com.stacksync.commons.models.abe.ABEMetaComponent;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.SyncMetadata;
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
import java.util.UUID;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author javigd
 */
public class PostgresqlABEItemDAO extends PostgresqlItemDAO implements ABEItemDAO {
    
	private static final Logger logger = Logger
			.getLogger(PostgresqlItemDAO.class.getName());

        public PostgresqlABEItemDAO(Connection connection) {
            super(connection);
        }
        
	@Override
	public void add(Item item) throws DAOException {
                ABEItem it = (ABEItem) item;
                
		if (!it.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}
           
		Object[] values = { it.getWorkspace().getId(),
				it.getLatestVersion(), it.getParentId(),
				it.getFilename(), it.getMimetype(), it.isFolder(),
				it.getClientParentFileVersion(), it.getCipherSymKey() };

		String query = "INSERT INTO item ( workspace_id, latest_version, parent_id,"
				+ " filename, mimetype, is_folder,"
				+ " client_parent_file_version, encrypted_dek ) "
				+ "VALUES ( ?::uuid, ?, ?, ?, ?, ?, ?, ? )";

		Long id = (Long)executeUpdate(query, values);
                
                for (ABEMetaComponent metaComponent : it.getAbeComponents()) {
                        Object[] abeValues = { id, metaComponent.getAttributeId(),
                                            metaComponent.getEncryptedPKComponent(),
                                            metaComponent.getVersion(), it.getLatestVersion()};

                        String abeQuery = "INSERT INTO abe_component ( item_id, attribute, "
                                        + "encrypted_pk_component,"
                                        + " version, item_version ) "
                                        + "VALUES ( ?, ?, ?, ?, ? )";
 
                        executeUpdate(abeQuery, abeValues);
                }

		if (id != null) {
			it.setId(id);
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
                ABEItem it = (ABEItem) item;
		
                if (it.getId() == null || !it.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}

		Long parentId = it.getParentId();
		// If id == 0 means parent is null!
		if (parentId != null && parentId == 0) {
			parentId = null;
		}

		Object[] values = { it.getWorkspace().getId(),
				it.getLatestVersion(), parentId, it.getFilename(),
				it.getMimetype(), item.isFolder(),
				it.getClientParentFileVersion(),  it.getCipherSymKey(), it.getId() };

		String query = "UPDATE item SET " + "workspace_id = ?::uuid, "
				+ "latest_version = ?, " + "parent_id = ?, " + "filename = ?, "
				+ "mimetype = ?, " + "is_folder = ?, "
				+ "client_parent_file_version = ?, encrypted_dek = ? " + "WHERE id = ?";

		executeUpdate(query, values);
                
                for (ABEMetaComponent metaComponent : it.getAbeComponents()) {
                        Object[] abeValues = { it.getId(), metaComponent.getAttributeId(),
                                            metaComponent.getEncryptedPKComponent(),
                                            metaComponent.getVersion(), it.getLatestVersion()};

                        String abeQuery = "INSERT INTO abe_component ( item_id, attribute, "
                                        + "encrypted_pk_component,"
                                        + " version, item_version ) "
                                        + "VALUES ( ?, ?, ?, ?, ? )";
 
                        executeUpdate(abeQuery, abeValues);
                }
	}
        
        @Override
        public List<SyncMetadata> getABEItemsByWorkspaceId(UUID workspaceId) throws DAOException {
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
                            + " i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.encrypted_dek, "
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
                            + " c.attribute as attribute_id, c.encrypted_pk_component, c.version as abe_version, "
                            + " q.*, get_chunks(q.version_id) AS chunks "
                            + " FROM q  "
                            + " LEFT OUTER JOIN abe_component c "
                            + " ON c.item_id = q.item_id "
                            + " AND c.item_version = q.version "
                            + " ORDER BY level_array ASC";

            ResultSet result = null;
            List<SyncMetadata> items;
            
            try {
                    result = executeQuery(query, values);
                    
                    items = new ArrayList<SyncMetadata>();
                    Long lastItemId = 0L;
                    
                    while (result.next()) {
                            ABEItemMetadata item = DAOUtil
                                            .getABEItemMetadataFromResultSet(result);
                            if (!lastItemId.equals(item.getId())) {
                                items.add(item);
                            } else {
                                ABEItemMetadata it = (ABEItemMetadata)items.get(items.size() - 1);
                                it.addAbeComponent(item.getAbeComponents().get(0));
                                items.set(items.size() - 1, it);
                            }
                            lastItemId = item.getId();
                    }

            } catch (SQLException e) {
                    logger.error(e);
                    throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
            }
            
            return items;
        }
        
                
        @Override
        public ArrayList<Long> getItemsIDWithAttributeInWorkspace(UUID workspaceId, String attribute) throws DAOException{
            ArrayList<Long> itemsIds = new ArrayList<Long>();
            
            Object[] values = { workspaceId, attribute };
            
            String query = "SELECT abe_component.item_id FROM abe_component INNER JOIN item ON item.id=abe_component.item_id WHERE item.workspace_id=? AND abe_component.attribute=?";
            
            ResultSet result = executeQuery(query, values);
            
            try {
                while(result.next()){
                    itemsIds.add(result.getLong("item_id"));
                }
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlItemDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return itemsIds;
        }
        
        @Override
        public void setNotUpdatedItemInWorkspace(Long itemId) throws DAOException{

            Object[] values = { itemId };
            
            String query = "UPDATE item SET " + "updated = false WHERE id=?";
            
            executeUpdate(query, values);
            
        }
        
        @Override
        public void setUpdatedItem(Long itemId) throws DAOException{
            
            Object[] values = {itemId };
            
            String query = "UPDATE item SET " + "updated = true WHERE id=?";
            
            executeUpdate(query, values);
            
        }
        
        @Override
        public HashMap<Long,ArrayList<ABEMetaComponent>> getNotUpdatedABEComponentsInWorkspace(UUID workspaceId) throws DAOException{
            
            Object[] values = { workspaceId };
            
            String query = "SELECT workspace_attribute_universe.id, abe_component.item_id, abe_component.attribute, abe_component.version, abe_component.encrypted_pk_component FROM"
                    + " item INNER JOIN abe_component ON item.id=abe_component.item_id INNER JOIN workspace_attribute_universe ON workspace_attribute_universe.attribute = abe_component.attribute WHERE item.workspace_id=? AND item.updated = false";

            ResultSet result = executeQuery(query, values);
            
            HashMap<Long,ArrayList<ABEMetaComponent>> fileComps = new HashMap<Long,ArrayList<ABEMetaComponent>>();
            
            try {
                
                while(result.next()){
                    
                    if(fileComps.get(result.getLong("item_id"))==null){
                        fileComps.put(result.getLong("item_id"), new ArrayList<ABEMetaComponent>());        
                    }
                                         
                    fileComps.get(result.getLong("item_id")).add(new ABEMetaComponent(result.getLong("id"), result.getString("attribute"), result.getBytes("encrypted_pk_component"), result.getLong("version")));
                }
                
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlABEItemDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return fileComps;
            
        }
        
        @Override
        public void setUpdatedABEComponents(HashMap<Long,ArrayList<ABEMetaComponent>> fileComponents) throws DAOException{
            
            
            for(Long file:fileComponents.keySet()){
                
                for(ABEMetaComponent component:fileComponents.get(file)){
                    
                    Object[] values = {component.getEncryptedPKComponent(), component.getVersion(), file, component.getAttributeId()};
                    
                    String query = "UPDATE abe_component SET encrypted_pk_component=?, version=? WHERE item_id=? AND attribute=?";
                    
                    executeUpdate(query, values);
                            
                }
               
                setUpdatedItem(file);
            }
            
            
            
            
        }
}
