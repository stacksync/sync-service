/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.UnauthorizedException;
import com.stacksync.syncservice.handler.UnshareData;
import org.apache.log4j.Logger;
import org.infinispan.atomic.Distribute;
import org.infinispan.atomic.Distributed;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */


@Distributed(key="id")
public class InfinispanDAO implements GlobalDAO{

   private static final Logger logger = Logger.getLogger(InfinispanDAO.class.getName());

   @Distribute(key = "deviceIndex")
   public static Map<UUID,DeviceRMI> deviceMap = new ConcurrentHashMap<>();

   @Distribute(key = "userIndex")
   public static Map<UUID,UserRMI> userMap = new ConcurrentHashMap<>();

   @Distribute(key = "mailIndex")
   public static Map<UUID,String> mailMap = new ConcurrentHashMap<>();

   @Distribute(key = "workspaceIndex")
   public static Map<UUID,WorkspaceRMI> workspaceMap = new ConcurrentHashMap<>();

   // FIXME to remove (requires some API changes)
   private static Map<Long, ItemRMI> itemMap = new ConcurrentHashMap<>();

   public UUID id;

   @Deprecated
   public InfinispanDAO(){}

   public InfinispanDAO(UUID id){
      this.id = id;
   }

   public String toString(){
      return "InfinispanDAO#"+id;
   }

   // Device

   @Override
   public DeviceRMI get(UUID id) {
      return deviceMap.get(id);
   }

   @Override
   public void add(DeviceRMI device) {
      deviceMap.put(device.getId(), device);
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
   public ItemRMI findById(Long id) {
      return itemMap.get(id);
   }

   @Override
   public void add(ItemRMI item) {
      itemMap.put(item.getId(), item);
   }

   @Override
   public void update(ItemRMI item) {
      // nothing to do
   }

   @Override
   public void delete(Long id) {
      itemMap.remove(id);
   }

   // metadata

   @Override
   public List<ItemMetadataRMI> getItemsByWorkspaceId(UUID workspaceId) {
      return workspaceMap.get(workspaceId).getItemsMetadata();
   }

   @Override
   public List<ItemMetadataRMI> getItemsById(Long id) {
      throw new IllegalArgumentException("NYI");
   }

   @Override
   public ItemMetadataRMI findById(Long id, Boolean includeList, Long version, Boolean includeDeleted,
         Boolean includeChunks)  {
      ItemRMI itemRMI = itemMap.get(id);
      assert itemRMI!=null;
      return workspaceMap.get(itemRMI.getWorkspaceId()).findById(id, includeList, version, includeDeleted, includeChunks);
   }

   @Override
   public ItemMetadataRMI findByUserId(UUID serverUserId, Boolean includeDeleted) {
      WorkspaceRMI userWorkspace = getDefaultWorkspaceByUserId(serverUserId);
      return userWorkspace.getItemsMetadata(includeDeleted);
   }

   @Override
   public ItemMetadataRMI findItemVersionsById(Long id) {
      throw new IllegalArgumentException("NYI");
   }

   @Override
   public List<String> migrateItem(Long itemId, UUID workspaceId) {
      throw new IllegalArgumentException("NYI");
   }

   // Itemversion

   @Override
   public void add(ItemVersionRMI itemVersion) {
      ItemRMI itemRMI = itemMap.get(itemVersion.getItemId());
      assert itemRMI!=null : "Unable to find "+itemVersion.getItemId();
      itemRMI.addVersion(itemVersion);
   }

   @Override
   public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version) {
      ItemRMI item = itemMap.get(id);
      ItemVersionRMI itemVersion = item.getVersion(version);
      assert item!=null && itemVersion != null;
      return ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item, itemVersion);
   }

   @Override
   public void insertChunk(Long itemVersionId, Long chunkId, Integer order) {
      throw new IllegalArgumentException("NYI");
   }

   @Override
   public void insertChunks(ItemVersionRMI itemVersionRMI, List<ChunkRMI> chunks) {
      itemVersionRMI.addChunks(chunks);
   }

   @Override
   public List<ChunkRMI> findChunks(Long itemVersionId) {
      throw new IllegalArgumentException("NYI");
   }

   @Override
   public void update(ItemVersionRMI itemVersion) {
      throw new IllegalArgumentException("NYI");
   }

   @Override
   public void delete(ItemVersionRMI itemVersion) {
      throw new IllegalArgumentException("NYI");
   }

   // User

   @Override
   public UserRMI findById(UUID id) {
      return userMap.get(id);
   }

   @Override
   public UserRMI getByEmail(String email)  {
      for(UUID uuid: userMap.keySet()) {
         if (mailMap.get(uuid).equals(email))
            return userMap.get(uuid);
      }
      return null;
   }

   @Override
   public List<UserRMI> findAll() {
      return new ArrayList<>(userMap.values());
   }

   @Override
   public List<UserRMI> findByItemId(Long clientFileId) {
      throw new IllegalArgumentException("NYI");
   }

   @Override
   public void add(UserRMI user) {
      userMap.put(user.getId(), user);
      mailMap.put(user.getId(), user.getEmail());
   }

   @Override
   public void update(UserRMI user) {
      // nothing to do
   }

   @Override
   public void deleteUser(UUID id) {
      userMap.remove(id);
      mailMap.remove(id);
   }

   // Workspace

   @Override
   public WorkspaceRMI getById(UUID id) {
      return workspaceMap.get(id);
   }

   @Override
   public List<WorkspaceRMI> getByUserId(UUID userId) {
      List<WorkspaceRMI> ret = new ArrayList<>();
      UserRMI user = userMap.get(userId);
      for (UUID uuid : user.getWorkspaces()){
         ret.add(workspaceMap.get(uuid));
      }
      return ret;
   }

   @Override
   public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId) {
      UserRMI user = userMap.get(userId);
      List<UUID> list = user.getWorkspaces();
      if (list.isEmpty())
         throw new IllegalArgumentException("No workspace for "+userId);
      return workspaceMap.get(list.get(0));
   }

   @Override
   public WorkspaceRMI getByItemId(Long itemId) {
      return workspaceMap.get(itemMap.get(itemId).getWorkspaceId());
   }

   @Override
   public void add(WorkspaceRMI workspace) {
      workspaceMap.putIfAbsent(workspace.getId(), workspace);
   }

   @Override
   public void update(UserRMI user, WorkspaceRMI workspace) {
      // nothing to do
   }

   @Override
   public void addUser(UserRMI user, WorkspaceRMI workspace) {
      workspace.addUser(user);
   }

   @Override
   public void deleteUser(UserRMI user, WorkspaceRMI workspace) {
      workspace.removeUser(user.getId());
   }

   @Override
   public void deleteWorkspace(UUID id) {
      workspaceMap.remove(id);
   }

   @Override
   public List<UserRMI> getMembersById(UUID workspaceId) {
      return workspaceMap.get(workspaceId).getUsers();
   }

   @Override
   public List<CommitInfo> doCommit(UUID userId, UUID workspaceId, UUID deviceId, List<ItemMetadataRMI> items)
         throws DAOException {

      List<CommitInfo> responseObjects = new ArrayList<>();
      try {

         UserRMI user = userMap.get(userId);
         if (user==null) throw new DAOException("invalid user "+userId);

         DeviceRMI device = deviceMap.get(deviceId);
         if (device==null) throw new DAOException("invalid device "+deviceId);

         WorkspaceRMI workspace = workspaceMap.get(workspaceId);
         if (workspace==null) throw new DAOException("invalid workspace "+deviceId);

         if (!workspace.isOwner(user))
            throw new UnauthorizedException("invalid rights");

         HashMap<Long, Long> tempIds = new HashMap<>();

         for (ItemMetadataRMI itemMetadata : items) {

            ItemMetadataRMI objectResponse;

            if (itemMetadata.getParentId() != null) {
               Long parentId = tempIds.get(itemMetadata.getParentId());
               if (parentId != null) {
                  itemMetadata.setParentId(parentId);
               }
            }

            // if the itemMetadata does not have ID but has a TempID, maybe it was set
            if (itemMetadata.getId() == null && itemMetadata.getTempId() != null) {
               Long newId = tempIds.get(itemMetadata.getTempId());
               if (newId != null) {
                  itemMetadata.setId(newId);
               }
            }

            workspace.add(itemMetadata);

            if (itemMetadata.getTempId() != null) {
               tempIds.put(itemMetadata.getTempId(), itemMetadata.getId());
            }

            objectResponse = itemMetadata;

            responseObjects.add(new CommitInfo(itemMetadata.getVersion(), true,
                  objectResponse.toMetadataItem()));
         }

      }catch(Exception e) {
         e.printStackTrace();
         return responseObjects;
      }

      return responseObjects;

   }

   @Override
   public UnshareData dosharedFolder(UserRMI user, List<String> emails, ItemRMI item, boolean isEncrypted)
         throws ShareProposalNotCreatedException {

      UnshareData response;

      if (item == null || !item.isFolder()) {
         throw new ShareProposalNotCreatedException("No folder found with the given ID.");
      }

      // Get the workspace
      WorkspaceRMI sourceWorkspace = workspaceMap.get(id);
      if (sourceWorkspace == null) {
         throw new ShareProposalNotCreatedException("Workspace not found.");
      }
      if (!sourceWorkspace.isShared()) {
         throw new ShareProposalNotCreatedException("This workspace is not shared.");
      }

      // Check the addressees
      List<UserRMI> addressees = new ArrayList<>();
      for (String email : emails) {
         UserRMI addressee;
         try {
            addressee = getByEmail(email);
            if (addressee.getId().equals(sourceWorkspace.getOwner())){
               logger.warn(String.format("Email '%s' corresponds with owner of the folder. ", email));
               throw new ShareProposalNotCreatedException("Email "+email+" corresponds with owner of the folder.");
            }

            if (!addressee.getId().equals(user.getId())) {
               addressees.add(addressee);
            }


         } catch (IllegalArgumentException e) {
            logger.error(e);
            throw new ShareProposalNotCreatedException(e);
         } catch (Exception e) {
            logger.warn(String.format("Email '%s' does not correspond with any user. ", email), e);
         }
      }

      if (addressees.isEmpty()) {
         throw new ShareProposalNotCreatedException("No addressees found");
      }

      // remove users from workspace
      List<UserRMI> usersToRemove = new ArrayList<>();
      for (UserRMI userToRemove : addressees) {
         for (UserRMI member : sourceWorkspace.getUsers()) {
            if (member.getEmail().equals(userToRemove.getEmail())) {
               usersToRemove.add(userToRemove);
               break;
            }
         }
      }

      if (usersToRemove.size()==sourceWorkspace.getUsers().size()) {

         response = null;

//         // All members have been removed from the workspace
//         WorkspaceRMI defaultWorkspace;
//         try {
//            //Always the last member of a shared folder should be the owner
//            defaultWorkspace = globalDAO.getDefaultWorkspaceByUserId(sourceWorkspace.getOwner());
//         } catch (Exception e) {
//            logger.error(e);
//            throw new ShareProposalNotCreatedException("Could not get default workspace");
//         }
//
//         // Migrate files to new workspace
//         List<String> chunks;
//         try {
//            chunks = globalDAO.migrateItem(item.getId(), defaultWorkspace.getId());
//         } catch (Exception e) {
//            logger.error(e);
//            throw new new ShareProposalNotCreatedException(e);
//         }
//
//         // Move chunks to new container
//         for (String chunkName : chunks) {
//            try {
//               storageManager.copyChunk(sourceWorkspace, defaultWorkspace, chunkName);
//            } catch (Exception e) {
//               logger.error(e);
//               throw new ShareProposalNotCreatedException(e);
//            }
//         }
//
//         // delete workspace
//         try {
//            workspaceDAO.deleteWorkspace(sourceWorkspace.getId());
//         } catch (Exception e) {
//            logger.error(e);
//            throw new ShareProposalNotCreatedException(e);
//         }
//
//         // delete container from swift
//         try {
//            storageManager.deleteWorkspace(sourceWorkspace);
//         } catch (Exception e) {
//            logger.error(e);
//            throw new ShareProposalNotCreatedException(e);
//         }
//
//         response = new UnshareData(usersToRemove, sourceWorkspace, true);

      } else {

         sourceWorkspace.removeUsers(usersToRemove);
         response = new UnshareData(usersToRemove, sourceWorkspace, false);

      }

      return response;

   }

}
