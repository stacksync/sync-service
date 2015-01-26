package com.stacksync.syncservice.db.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.SharingProposal;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.DAOUtil;
import com.stacksync.syncservice.db.SharingProposalDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;

public class PostgresqlSharingProposalDAO extends PostgresqlDAO implements SharingProposalDAO{
	
	private static final Logger logger = Logger
			.getLogger(PostgresqlItemDAO.class.getName());
	
	public PostgresqlSharingProposalDAO(Connection connection) {
		super(connection);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SharingProposal findById(Long id) throws DAOException {
		ResultSet resultSet = null;
		SharingProposal sharingProposal = null;

		String query = "SELECT * FROM cloudspaces_sharing_proposal WHERE id = ?";

		try {
			resultSet = executeQuery(query, new Object[] { id });

			if (resultSet.next()) {
				sharingProposal = DAOUtil.getSharingProposalFromResultSet(resultSet);
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

		return sharingProposal;
	}
	

	@Override
	public void add(SharingProposal sharingProposal) throws DAOException {
		if (!sharingProposal.isValid()) {
			throw new IllegalArgumentException("SharingProposal attributes not set");
		}

		Object[] values = { sharingProposal.getKey(),
				sharingProposal.getIsLocal(), sharingProposal.getService(),
				sharingProposal.getResourceUrl(), sharingProposal.getOwner(), sharingProposal.getFolder(),
				sharingProposal.getWriteAccess(), sharingProposal.getRecipient(), sharingProposal.getCallback(), 
				sharingProposal.getProtocolVersion(), sharingProposal.getStatus()};

		String query = "INSERT INTO cloudspaces_sharing_proposal ( key, service_id, resource_url,"
				+ " owner_id, folder_id, write_access,"
				+ " recipient, callback, protocol_version, status"
				+ "VALUES ( ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

		Long id = (Long)executeUpdate(query, values);

		if (id != null) {
			sharingProposal.setId(id);
		}
	}

	@Override
	public void update(SharingProposal sharingProposal) throws DAOException {
		if (sharingProposal.getId() == null || !sharingProposal.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}


		Object[] values = { sharingProposal.getKey(),
				sharingProposal.getIsLocal(), sharingProposal.getService(),
				sharingProposal.getResourceUrl(), sharingProposal.getOwner(),
				sharingProposal.getFolder(),sharingProposal.getWriteAccess(), 
				sharingProposal.getRecipient(), sharingProposal.getCallback(), 
				sharingProposal.getProtocolVersion(), sharingProposal.getStatus(), 
				sharingProposal.getId()};


		String query = "UPDATE item SET " + "key = ?::uuid, "
				+ "service_id = ?, " + "resource_url = ?, " + "owner_id = ?, "
				+ "folder_id = ?, " + "write_access = ?, "
				+ "recipient = ?," + "callback = ?, "+ "protocol_version = ?, "
				+ "status = ?" + "WHERE id = ?";

		executeUpdate(query, values);
		
	}

	@Override
	public void put(SharingProposal sharingProposal) throws DAOException {
		if (sharingProposal.getId() == null) {
			add(sharingProposal);
		} else {
			update(sharingProposal);
		}
	}

	@Override
	public void delete(SharingProposal sharingProposal) throws DAOException {
		// TODO Auto-generated method stub
		
	}
	

}
