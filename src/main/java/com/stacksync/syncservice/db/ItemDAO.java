package com.stacksync.syncservice.db;

import com.stacksync.commons.models.Item;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;

import java.util.List;
import java.util.UUID;

public interface ItemDAO {
	public Item findById(Long id) throws DAOException;

	public void add(Item item) throws DAOException;

	public void update(Item item) throws DAOException;

	public void put(Item item) throws DAOException;

	public void delete(Long id) throws DAOException;

	// ItemMetadata information
	public List<ItemMetadataRMI> getItemsByWorkspaceId(UUID workspaceId) throws DAOException;

	public List<ItemMetadataRMI> getItemsById(Long id) throws DAOException;

	public ItemMetadataRMI findById(Long id, Boolean includeList, Long version, Boolean includeDeleted,
         Boolean includeChunks) throws DAOException;

	public ItemMetadataRMI findByUserId(UUID serverUserId, Boolean includeDeleted) throws DAOException;

	public ItemMetadataRMI findItemVersionsById(Long id) throws DAOException;
	
	public List<String> migrateItem(Long itemId, UUID workspaceId) throws DAOException;

}
