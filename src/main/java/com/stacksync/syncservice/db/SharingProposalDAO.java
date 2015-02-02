package com.stacksync.syncservice.db;

import java.util.UUID;

import com.stacksync.commons.models.SharingProposal;
import com.stacksync.syncservice.exceptions.dao.DAOException;

//         db_table = 'cloudspaces_sharing_proposal'

public interface SharingProposalDAO {
	public SharingProposal findById(Long id) throws DAOException;
	
	public SharingProposal findByKey(UUID key) throws DAOException;
	
	public void add(SharingProposal sharingProposal) throws DAOException;
	
	public void update(SharingProposal sharingProposal) throws DAOException;
	
	public void put(SharingProposal sharingProposal) throws DAOException;
	
	public void delete(SharingProposal sharingProposal) throws DAOException;
	
}