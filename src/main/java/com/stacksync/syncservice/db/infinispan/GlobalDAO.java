package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.UnshareData;

import java.util.List;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public interface GlobalDAO
      extends
      WorkspaceDAO,
      ItemDAO,
      ItemVersionDAO,
      UserDAO,
      DeviceDAO{

      List<CommitInfo> doCommit(UUID userId, UUID workspaceId, UUID deviceId, List<ItemMetadataRMI> items)
            throws DAOException;

      UnshareData dosharedFolder(UserRMI user, List<String> emails, ItemRMI item, boolean isEncrypted)
            throws ShareProposalNotCreatedException, UserNotFoundException;
}

