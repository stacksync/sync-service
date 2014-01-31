package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public interface ItemDAO {
	public Item findById(Long id) throws DAOException;

	public List<Item> findByWorkspaceId(long workspaceId) throws DAOException;

	public void add(Item item) throws DAOException;

	public void update(Item item) throws DAOException;

	public void put(Item item) throws DAOException;

	public void delete(Long id) throws DAOException;

	// ItemMetadata information
	public List<ItemMetadata> getItemsByWorkspaceId(Long workspaceId) throws DAOException;

	public List<ItemMetadata> getItemsById(Long id) throws DAOException;

	public ItemMetadata findById(Long id, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks) throws DAOException;

	public ItemMetadata findByServerUserId(String serverUserId, Boolean includeDeleted) throws DAOException;

	public ItemMetadata findItemVersionsById(Long id) throws DAOException;

}
