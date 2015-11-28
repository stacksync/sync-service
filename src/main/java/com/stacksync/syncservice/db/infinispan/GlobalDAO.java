package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;

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

      List<CommitInfo> doCommit(UserRMI user, WorkspaceRMI workspace,
            DeviceRMI device, List<ItemMetadataRMI> items) throws Exception;

}

