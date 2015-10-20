/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.models.*;
import com.stacksync.syncservice.db.infinispan.models.*;
import org.infinispan.atomic.AtomicObjectFactory;

import java.rmi.RemoteException;
import java.util.*;

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

   private final AtomicObjectFactory factory;
   private Map<UUID,DeviceRMI> deviceMap;
   private Map<UUID,UserRMI> userMap;
   private Map<UUID,UserRMI> mailMap;
   private Map<UUID,WorkspaceRMI> workspaceMap;
   private Map<Long,ItemRMI> itemMap;

   public InfinispanDAO(AtomicObjectFactory factory) {
      this.factory = factory;
      deviceMap = factory.getInstanceOf(HashMap.class,"deviceMap");
      userMap = factory.getInstanceOf(HashMap.class,"userMap");
      mailMap = factory.getInstanceOf(HashMap.class,"mailMap");
      workspaceMap = factory.getInstanceOf(HashMap.class,"workspaceMap");
      itemMap = factory.getInstanceOf(HashMap.class,"itemMap");
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
      itemMap.put(item.getId(),item);
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
   public List<ItemMetadata> getItemsByWorkspaceId(UUID workspaceId) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public List<ItemMetadata> getItemsById(Long id) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public ItemMetadata findById(Long id, Boolean includeList, Long version, Boolean includeDeleted,
         Boolean includeChunks) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public ItemMetadata findByUserId(UUID serverUserId, Boolean includeDeleted) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public ItemMetadata findItemVersionsById(Long id) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public List<String> migrateItem(Long itemId, UUID workspaceId) throws RemoteException {
      throw new RemoteException("NYI");
   }

   // Itemversion

   @Override
   public void add(ItemVersionRMI itemVersion) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public ItemMetadata findByItemIdAndVersion(Long id, Long version) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override public void insertChunks(long itemId, List<ChunkRMI> chunks, long itemVersionId) throws RemoteException {
      throw new RemoteException("NYI");
   }

   @Override
   public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException {
      return null;  // TODO: Customise this generated block
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
      UserRMI user = userMap.get(userId);
      return (List) user.getWorkspaces();
   }

   @Override
   public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId) throws RemoteException {
      UserRMI user = userMap.get(userId);
      List<UUID> list = user.getWorkspaces();
      return workspaceMap.get(list.get(0));
   }

   @Override
   public WorkspaceRMI getByItemId(Long itemId) throws RemoteException {
      throw new RemoteException("NIY");
   }

   @Override
   public void add(WorkspaceRMI workspace) throws RemoteException {
      workspaceMap.put(workspace.getId(),workspace);
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
      throw new RemoteException("NIY");
   }

   @Override
   public List<UserWorkspaceRMI> getMembersById(UUID workspaceId) throws RemoteException {
      throw new RemoteException("NIY");
   }


}
