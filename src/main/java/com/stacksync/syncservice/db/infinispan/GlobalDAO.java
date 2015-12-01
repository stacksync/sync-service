package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.handler.UnshareData;

import java.util.List;

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

      List<CommitInfo> doCommit(UserRMI user, WorkspaceRMI workspace, DeviceRMI device, List<ItemMetadataRMI> items);

      UnshareData dosharedFolder(UserRMI user, List<String> emails, ItemRMI item, boolean isEncrypted)
            throws ShareProposalNotCreatedException, UserNotFoundException;
}

