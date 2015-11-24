/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.handler.Handler;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanDAO
      implements
      InfinispanWorkspaceDAO,
      InfinispanItemDAO,
      InfinispanItemVersionDAO,
      InfinispanUserDAO,
      InfinispanDeviceDAO {

   private Map<UUID,DeviceRMI> deviceMap;
   private Map<UUID,UserRMI> userMap;
   private Map<UUID,UserRMI> mailMap;
   private Map<UUID,WorkspaceRMI> workspaceMap;
   private Map<Long,ItemRMI> itemMap;
   private Map<Long,ItemVersionRMI> itemVersionMap;

   public InfinispanDAO(
         Map<UUID,DeviceRMI> deviceMap,
         Map<UUID,UserRMI> userMap,
         Map<UUID,UserRMI> mailMap,
         Map<UUID,WorkspaceRMI> workspaceMap,
         Map<Long,ItemRMI> itemMap,
         Map<Long,ItemVersionRMI> itemVersionMap){
      this.deviceMap = deviceMap;
      this.userMap = userMap;
      this.mailMap = mailMap;
      this.workspaceMap = workspaceMap;
      this.itemMap = itemMap;
      this.itemVersionMap = itemVersionMap;
   }

   // Device

   @Override
   public DeviceRMI get(UUID id) {
      return deviceMap.get(id);
   }

   @Override
   public void add(DeviceRMI device) {
      deviceMap.put(device.getId(),device);
   }

   @Override
   public void update(DeviceRMI device) {
      // nothing to do
   }

   @Override
   public void deleteDevice(UUID id) {
      deviceMap.remove(id);
   }

   // Item

   @Override
   public ItemRMI findById(Long id) throws RemoteException {
      return itemMap.get(id);
   }

   @Override
   public void add(ItemRMI item) throws RemoteException {
      itemMap.put(item.getId(), item);
   }

   @Override
   public void update(ItemRMI item) throws RemoteException {
      // nothing to do
   }

   @Override
   public void put(ItemRMI item) throws RemoteException {
      add(item); // FIXME
   }

   @Override
   public void delete(Long id) throws RemoteException {
      itemMap.remove(id);
   }

   // metadata

   @Override
   public List<ItemMetadataRMI> getItemsByWorkspaceId(UUID workspaceId) throws RemoteException {
      return workspaceMap.get(workspaceId).getItemsMetadata();
   }

   @Override
   public List<ItemMetadataRMI> getItemsById(Long id) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public ItemMetadataRMI findById(Long id, Boolean includeList, Long version, Boolean includeDeleted,
         Boolean includeChunks) throws RemoteException {
      ItemRMI itemRMI = itemMap.get(id);
      assert itemRMI!=null;
      return itemRMI.getWorkspace().findById(id, includeList, version, includeDeleted, includeChunks);
   }

   @Override
   public ItemMetadataRMI findByUserId(UUID serverUserId, Boolean includeDeleted) throws RemoteException {
      ItemMetadataRMI rootMetadata = new ItemMetadataRMI();
      rootMetadata.setIsFolder(true);
      rootMetadata.setFilename("root");
      rootMetadata.setIsRoot(true);
      rootMetadata.setVersion(new Long(0));

      WorkspaceRMI userWorkspace = getDefaultWorkspaceByUserId(serverUserId);
      System.out.println("workspace " + userWorkspace + " with " + userWorkspace.getItemsMetadata());
      for (ItemMetadataRMI itemMetadata : userWorkspace.getItemsMetadata()) {
         if (itemMetadata.getStatus().compareTo(Handler.Status.DELETED.toString()) == 0) {
            if (includeDeleted) {
               rootMetadata.addChild(itemMetadata);
            }
         } else {
            rootMetadata.addChild(itemMetadata);
         }
      }

      return rootMetadata;
   }

   @Override
   public ItemMetadataRMI findItemVersionsById(Long id) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public List<String> migrateItem(Long itemId, UUID workspaceId) throws RemoteException {
      throw new RemoteException("NYI");
   }

   // Itemversion

   @Override
   public void add(ItemVersionRMI itemVersion) throws RemoteException {
      //System.out.println("Adding "+itemVersion.getId());
      ItemRMI itemRMI = itemMap.get(itemVersion.getItemId());
      itemRMI.addVersion(itemVersion);
      itemVersionMap.put(itemVersion.getId(), itemVersion);
   }

   @Override
   public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version) throws RemoteException {
      ItemRMI item = itemMap.get(id);
      ItemVersionRMI itemVersion = item.getVersion(version);
      assert item!=null && itemVersion != null;
      return ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item,itemVersion);
   }

   @Override
   public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public void insertChunks(long itemId, List<ChunkRMI> chunks, long itemVersionId) throws RemoteException {
      ItemRMI itemRMI = itemMap.get(itemId);
      ItemVersionRMI itemVersionRMI = itemRMI.getVersion(itemVersionId);
      if (itemVersionRMI==null)
         throw new RemoteException("Unable to find "+itemVersionId+"  in "+itemId);
      itemVersionRMI.addChunks(chunks);
   }

   @Override
   public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException {
      return itemVersionMap.get(itemVersionId).getChunks();
   }

   @Override
   public void update(ItemVersionRMI itemVersion) throws RemoteException {
      // TODO: Customise this generated block
   }

   @Override
   public void delete(ItemVersionRMI itemVersion) throws RemoteException {
      // TODO: Customise this generated block
   }

   // User

   @Override
   public UserRMI findById(UUID id) throws RemoteException {
      return userMap.get(id);
   }

   @Override
   public UserRMI getByEmail(String email) throws RemoteException {
      return mailMap.get(email);
   }

   @Override
   public List<UserRMI> findAll() throws RemoteException {
      return new ArrayList<>(userMap.values());
   }

   @Override
   public List<UserRMI> findByItemId(Long clientFileId) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public void add(UserRMI user) throws RemoteException {
      userMap.put(user.getId(),user);
      mailMap.put(user.getId(),user);
      System.out.println("Users: "+userMap.size());
   }

   @Override
   public void update(UserRMI user) throws RemoteException {
      // nothing to do
   }

   @Override
   public void deleteUser(UUID id) throws RemoteException {
      userMap.remove(id);
      mailMap.remove(id);
   }

   // Workspace

   @Override
   public WorkspaceRMI getById(UUID id) throws RemoteException {
      return workspaceMap.get(id);
   }

   @Override
   public List<WorkspaceRMI> getByUserId(UUID userId) throws RemoteException {
      List<WorkspaceRMI> ret = new ArrayList<>();
      UserRMI user = userMap.get(userId);
      for (UUID uuid : user.getWorkspaces()){
         ret.add(workspaceMap.get(uuid));
      }
      return ret;
   }

   @Override
   public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId) throws RemoteException {
      UserRMI user = userMap.get(userId);
      List<UUID> list = user.getWorkspaces();
      if (list.isEmpty())
         throw new RemoteException("No workspace for "+userId);
      return workspaceMap.get(list.get(0));
   }

   @Override
   public WorkspaceRMI getByItemId(Long itemId) throws RemoteException {
      return itemMap.get(itemId).getWorkspace();
   }

   @Override
   public void add(WorkspaceRMI workspace) throws RemoteException {
      workspaceMap.put(workspace.getId(),workspace);
      for (UUID userId: workspace.getUsers()) {
         UserRMI user = userMap.get(userId);
         user.addWorkspace(workspace.getId());
         //System.out.println("adding user "+user.getId()+" to "+workspace.getId());
      }
      System.out.println("Workspaces: "+workspaceMap.size());
   }

   @Override
   public void update(UserRMI user, WorkspaceRMI workspace) throws RemoteException {
      // nothing to do
   }

   @Override
   public void addUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException {
      workspace.addUser(user.getId());
   }

   @Override
   public void deleteUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException {
      workspace.removeUser(user.getId());
   }

   @Override public
   void deleteWorkspace(UUID id) throws RemoteException {
      workspaceMap.remove(id);
   }

   @Override
   public List<UserRMI> getMembersById(UUID workspaceId) throws RemoteException {
      List<UserRMI> ret = new ArrayList<>();
      for(UUID user : workspaceMap.get(workspaceId).getUsers()){
         ret.add(userMap.get(user));
      }
      return ret;
   }


}
