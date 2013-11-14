package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;

public class PostgresqlWorkspaceDAO extends PostgresqlDAO implements WorkspaceDAO {

	private static final Logger logger = Logger.getLogger(PostgresqlWorkspaceDAO.class.getName());

	public PostgresqlWorkspaceDAO(Connection connection) {
		super(connection);
	}

	@Override
	public Workspace findByPrimaryKey(Long workspaceID) throws DAOException {
		ResultSet resultSet = null;
		Workspace workspace = null;

		String query = "SELECT * FROM workspace WHERE id = ?";

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
	public Workspace findByName(String workspaceName) throws DAOException {

		Object[] values = { workspaceName };

		String query = "SELECT * FROM workspace WHERE client_workspace_name = ?";

		ResultSet result = null;
		Workspace workspace = null;

		try {
			result = executeQuery(query, values);

			// There is no more than one result due to workspace name is UNIQUE
			if (result.next()) {
				workspace = mapWorkspace(result);
			}
		} catch (SQLException e) {
			throw new DAOException(e);
		}

		return workspace;
	}

	@Override
	public Collection<Workspace> findAll() throws DAOException {
		ResultSet resultSet = null;
		Collection<Workspace> list = new ArrayList<Workspace>();

		String query = "SELECT * FROM CLIENTS";
		try {
			resultSet = executeQuery(query, null);

			while (resultSet.next()) {
				list.add(mapWorkspace(resultSet));
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}
		return list;
	}

	@Override
	public List<Workspace> findByUserCloudId(String userCloudId) throws DAOException {

		Object[] values = { userCloudId };

		String query = "SELECT * FROM workspace w " + "INNER JOIN user1 u ON w.owner_id=u.id " + "WHERE u.cloud_id=?";

		ResultSet result = null;
		List<Workspace> workspaces = new ArrayList<Workspace>();

		try {
			result = executeQuery(query, values);

			while (result.next()) {
				Workspace workspace = mapWorkspace(result);
				workspaces.add(workspace);
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return workspaces;
	}

	@Override
	public void add(Workspace workspace) throws DAOException {
		if (!workspace.isValid()) {
			throw new IllegalArgumentException("Workspace attributes not set");
		}

		Object[] values = { workspace.getClientWorkspaceName(), workspace.getLatestRevision(), workspace.getOwner().getId() };

		String query = "INSERT INTO workspace (client_workspace_name, latest_revision, owner_id) VALUES (?, ?, ?)";

		Long id = executeUpdate(query, values);

		if (id != null) {
			workspace.setId(id);
		}

	}

	@Override
	public void update(Workspace workspace) throws DAOException {
		if (workspace.getId() == null || !workspace.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		}

		Object[] values = { workspace.getClientWorkspaceName(), workspace.getLatestRevision(), workspace.getOwner().getId(), workspace.getId() };

		String query = "UPDATE workspace SET client_workspace_name = ?, latest_revision = ?, owner_id = ? WHERE id = ?";

		try {
			executeUpdate(query, values);
		} catch (DAOException e) {
			logger.error(e);
			throw new DAOException(e);
		}
	}

	@Override
	public void delete(Long workspaceID) throws DAOException {
		Object[] values = { workspaceID };

		String query = "DELETE FROM workspace WHERE id = ?";

		executeUpdate(query, values);
	}

	private Workspace mapWorkspace(ResultSet result) throws SQLException {
		Workspace workspace = new Workspace();
		workspace.setId(result.getLong("id"));
		workspace.setClientWorkspaceName(result.getString("client_workspace_name"));
		workspace.setLatestRevision(result.getInt("latest_revision"));

		User owner = new User();
		owner.setId(result.getLong("owner_id"));

		workspace.setOwner(owner);

		return workspace;
	}

	@Override
	public Long getPrimaryKey(String workspaceName) throws DAOException {
		Object[] values = { workspaceName };

		String query = "SELECT id FROM workspace WHERE client_workspace_name = ?";

		ResultSet result = null;
		Long workspaceId = null;

		try {
			result = executeQuery(query, values);

			// There is no more than one result due to workspace name is UNIQUE
			if (result.next()) {
				workspaceId = result.getLong("id");
			} else {
				// TODO error, no ha encontrado nada el perroo
				// throw workspace not found??
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return workspaceId;
	}

	@Override
	public void addUser(User user, Workspace workspace, String path) throws DAOException {
		if (!user.isValid()) {
			throw new IllegalArgumentException("User attributes not set");
		} else if (!workspace.isValid()) {
			throw new IllegalArgumentException("Workspace attributes not set");
		}

		Object[] values = { workspace.getId(), user.getId(), path };

		String query = "INSERT INTO workspace_user (workspace_id, user_id, client_workspace_path) VALUES (?, ?, ?)";

		executeUpdate(query, values);
	}

}
