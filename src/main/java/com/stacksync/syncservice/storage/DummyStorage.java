package com.stacksync.syncservice.storage;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;

/**
 * @author Pierre Sutra
 */
public class DummyStorage extends StorageManager {

   @Override
   public void login() throws Exception {}

   @Override
   public void createNewWorkspace(WorkspaceRMI workspace) throws Exception {}

   @Override
   public void removeUserToWorkspace(UserRMI owner, UserRMI user, WorkspaceRMI workspace) throws Exception {}

   @Override
   public void grantUserToWorkspace(UserRMI owner, UserRMI user, WorkspaceRMI workspace) throws Exception {}

   @Override
   public void copyChunk(WorkspaceRMI sourceWorkspace, WorkspaceRMI destinationWorkspace, String chunkName)
         throws Exception {}

   @Override
   public void deleteWorkspace(WorkspaceRMI workspace) throws Exception {}

}
