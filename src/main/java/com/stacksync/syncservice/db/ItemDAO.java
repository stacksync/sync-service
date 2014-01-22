package com.stacksync.syncservice.db;

import java.util.List;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Item;
import com.stacksync.syncservice.models.ItemMetadata;

public interface ItemDAO {
	public Item findById(Long id) throws DAOException;

	public List<Item> findByWorkspaceId(long workspaceId) throws DAOException;

	public List<Item> findByWorkspaceName(String workspaceName) throws DAOException;

	public void add(Item item) throws DAOException;

	public void update(Item item) throws DAOException;

	public void put(Item item) throws DAOException;

	public void delete(Long id) throws DAOException;

	// ItemMetadata information
	public List<ItemMetadata> getItemsByWorkspaceName(String workspaceName) throws DAOException;

	public List<ItemMetadata> getItemsById(Long id) throws DAOException;

	public ItemMetadata findById(Long id, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks) throws DAOException;

	public ItemMetadata findByServerUserId(String serverUserId, Boolean includeDeleted) throws DAOException;

	public ItemMetadata findItemVersionsById(Long id) throws DAOException;

}
