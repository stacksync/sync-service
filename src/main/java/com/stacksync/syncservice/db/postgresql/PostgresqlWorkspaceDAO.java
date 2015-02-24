package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.UserWorkspace;
import com.stacksync.commons.models.Workspace;

import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.dao.NoResultReturnedDAOException;

public class PostgresqlWorkspaceDAO extends PostgresqlDAO implements WorkspaceDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlWorkspaceDAO.class.getName());

	public PostgresqlWorkspaceDAO(Connection connection) {
		super(connection);
	}

	@Override
	public Workspace getById(UUID workspaceID) throws DAOException {
		ResultSet resultSet = null;
		Workspace workspace = null;

		String query = "SELECT * FROM workspace w INNER JOIN workspace_user wu ON wu.workspace_id = w.id WHERE w.id = ?::uuid";

		try {
			resultSet = executeQuery(query, new Object[] { workspaceID });

			if (resultSet.next()) {
				workspace = mapWorkspace(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return workspace;
	}

	@Override
	public List<Workspace> getByUserId(UUID userId) throws DAOException {

		Object[] values = { userId };

		String query = "SELECT w.*, wu.* FROM workspace w "
				+ " INNER JOIN workspace_user wu ON wu.workspace_id = w.id "
				+ " WHERE wu.user_id=?::uuid";

		ResultSet result = null;
		List<Workspace> workspaces = new ArrayList<Workspace>();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				Workspace workspace = mapWorkspace(result);
				workspaces.add(workspace);
			}

			if (workspaces.isEmpty()) {
				throw new NoResultReturnedDAOException(DAOError.WORKSPACES_NOT_FOUND);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return workspaces;
	}
	
	@Override
	public Workspace getDefaultWorkspaceByUserId(UUID userId) throws DAOException {

		Object[] values = { userId };

		String query = "SELECT w.*, wu.* FROM workspace w "
			 + " INNER JOIN workspace_user wu ON wu.workspace_id = w.id " 
			 + " WHERE w.owner_id=?::uuid AND w.is_shared = false LIMIT 1";

		ResultSet result = null;
		Workspace workspace;

		try {
			result = executeQuery(query, values);

			if (result.next()) {
				workspace = mapWorkspace(result);
			}else {
				throw new NoResultReturnedDAOException(DAOError.WORKSPACES_NOT_FOUND);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return workspace;
	}

	@Override
	public void add(Workspace workspace) throws DAOException {
		if (!workspace.isValid()) {
			throw new IllegalArgumentException("Workspace attributes not set");
		}

		Object[] values = { workspace.getLatestRevision(), workspace.getOwner().getId(), workspace.isShared(), workspace.isEncrypted(),
				workspace.getSwiftContainer(), workspace.getSwiftUrl() };

		String query = "INSERT INTO workspace (latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url) VALUES (?, ?, ?, ?, ?, ?)";

		UUID id = (UUID)executeUpdate(query, values);

		if (id != null) {
			workspace.setId(id);
		}

	}

	@Override
	public void update(User user, Workspace workspace) throws DAOException {
		if (workspace.getId() == null || user.getId() == null) {
			throw new IllegalArgumentException("Attributes not set");
		}

		Long parentItemId = null;
		if (workspace.getParentItem() != null) {
			parentItemId = workspace.getParentItem().getId();
		}

		Object[] values = { workspace.getName(), parentItemId, workspace.getId(), user.getId() };

		String query = "UPDATE workspace_user " + " SET workspace_name = ?, parent_item_id = ?, modified_at = now() "
				+ " WHERE workspace_id = ?::uuid AND user_id = ?::uuid";

		executeUpdate(query, values);
	}

	@Override
	public void delete(UUID workspaceID) throws DAOException {
		Object[] values = { workspaceID };

		String query = "DELETE FROM workspace WHERE id = ?::uuid";

		executeUpdate(query, values);
	}

	private Workspace mapWorkspace(ResultSet result) throws SQLException {
		Workspace workspace = new Workspace();
		workspace.setId(UUID.fromString(result.getString("id")));
		workspace.setLatestRevision(result.getInt("latest_revision"));
		workspace.setShared(result.getBoolean("is_shared"));
		workspace.setEncrypted(result.getBoolean("is_encrypted"));
		workspace.setName(result.getString("workspace_name"));

		workspace.setSwiftContainer(result.getString("swift_container"));
		workspace.setSwiftUrl(result.getString("swift_url"));

		Long parentItemId = result.getLong("parent_item_id");
		
		if (parentItemId == 0L){
			parentItemId = null;
		}
		
		workspace.setParentItem(new Item(parentItemId));
		
		User owner = new User();
		owner.setId(UUID.fromString(result.getString("owner_id")));

		workspace.setOwner(owner);

		return workspace;
	}

	@Override
	public void addUser(User user, Workspace workspace) throws DAOException {
		if (user == null || !user.isValid()) {
			throw new IllegalArgumentException("User not valid");
		} else if (workspace == null || !workspace.isValid()) {
			throw new IllegalArgumentException("Workspace not valid");
		}

		Long parentItemId = null;
		if (workspace.getParentItem() != null) {
			parentItemId = workspace.getParentItem().getId();
		}

		Object[] values = { workspace.getId(), user.getId(), workspace.getName(), parentItemId };

		String query = "INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES (?::uuid, ?::uuid, ?, ?)";

		executeUpdate(query, values);
	}
	
	@Override
	public void deleteUser(User user, Workspace workspace) throws DAOException {
		
		if (user == null || !user.isValid()) {
			throw new IllegalArgumentException("User not valid");
		} else if (workspace == null || !workspace.isValid()) {
			throw new IllegalArgumentException("Workspace not valid");
		}

		Object[] values = { workspace.getId(), user.getId() };

		String query = "DELETE FROM workspace_user WHERE workspace_id=?::uuid AND user_id=?::uuid";

		executeUpdate(query, values);
	}

	@Override
	public Workspace getByItemId(Long itemId) throws DAOException {
		ResultSet resultSet = null;
		Workspace workspace = null;

		String query = "SELECT * FROM workspace w " +
				" INNER JOIN workspace_user wu ON wu.workspace_id = w.id " +
				" INNER JOIN item i ON w.id = i.workspace_id " +
				" WHERE i.id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { itemId });

			if (resultSet.next()) {
				workspace = mapWorkspace(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return workspace;
	}
	
	@Override
	public List<UserWorkspace> getMembersById(UUID workspaceId) throws DAOException {
		ResultSet resultSet = null;
		List<UserWorkspace> users = new ArrayList<UserWorkspace>();

		String query = " SELECT u.*, CASE WHEN u.id=w.owner_id THEN True ELSE False END AS is_owner " +
			" , wu.created_at AS joined_at, wu.workspace_id " + 
			" FROM workspace w " + 
			" INNER JOIN workspace_user wu ON wu.workspace_id = w.id " +
			" INNER JOIN user1 u ON wu.user_id = u.id " +
			" WHERE w.id = ?::uuid";

		try {
			resultSet = executeQuery(query, new Object[] { workspaceId });

			while (resultSet.next()) {
				UserWorkspace userWorkspace = DAOUtil.getUserWorkspaceFromResultSet(resultSet);
				users.add(userWorkspace);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return users;
	}
	
}
