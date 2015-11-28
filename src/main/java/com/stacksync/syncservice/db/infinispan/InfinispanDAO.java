/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.Status;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
import org.infinispan.atomic.Distribute;
import org.infinispan.atomic.Distributed;

import java.time.Instant;
import java.util.*;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
@Distributed(key = "id")
public class InfinispanDAO implements GlobalDAO{

   @Distribute(key = "deviceIndex")
   public static Map<UUID,DeviceRMI> deviceMap = new HashMap<>();

   @Distribute(key = "userIndex")
   public static Map<UUID,UserRMI> userMap = new HashMap<>();

   @Distribute(key = "mailIndex")
   public static Map<UUID,UserRMI> mailMap = new HashMap<>();

   @Distribute(key = "workspaceIndex")
   public static Map<UUID,WorkspaceRMI> workspaceMap = new HashMap<>();

   @Distribute(key = "itemIndex")
   public static Map<Long,ItemRMI> itemMap = new HashMap<>();

   public UUID id;

   ;

   public InfinispanDAO(){
      this.id = UUID.randomUUID();
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
      return itemRMI.getWorkspace().findById(id, includeList, version, includeDeleted, includeChunks);
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
      return mailMap.get(email);
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
      userMap.put(user.getId(),user);
      mailMap.put(user.getId(), user);
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
      return itemMap.get(itemId).getWorkspace();
   }

   @Override
   public void add(WorkspaceRMI workspace) {
      workspaceMap.put(workspace.getId(), workspace);
      for (UUID userId: workspace.getUsers()) {
         UserRMI user = userMap.get(userId);
         user.addWorkspace(workspace.getId());
      }
   }

   @Override
   public void update(UserRMI user, WorkspaceRMI workspace) {
      // nothing to do
   }

   @Override
   public void addUser(UserRMI user, WorkspaceRMI workspace) {
      workspace.addUser(user.getId());
   }

   @Override
   public void deleteUser(UserRMI user, WorkspaceRMI workspace) {
      workspace.removeUser(user.getId());
   }

   @Override public
   void deleteWorkspace(UUID id) {
      workspaceMap.remove(id);
   }

   @Override
   public List<UserRMI> getMembersById(UUID workspaceId) {
      List<UserRMI> ret = new ArrayList<>();
      for(UUID user : workspaceMap.get(workspaceId).getUsers()){
         ret.add(userMap.get(user));
      }
      return ret;
   }

   @Override
   public List<CommitInfo> doCommit(UserRMI user, WorkspaceRMI workspace, DeviceRMI device,
         List<ItemMetadataRMI> items) throws CommitWrongVersion, CommitExistantVersion, CommitWrongVersionNoParent {

      HashMap<Long, Long> tempIds = new HashMap<>();

      if (!device.belongTo(user) || workspace.allow(user))
         throw new IllegalArgumentException("Wrong user");

      List<CommitInfo> responseObjects = new ArrayList<>();

      for (ItemMetadataRMI itemMetadata : items) {

         ItemMetadataRMI objectResponse;
         boolean committed;

         if (itemMetadata.getParentId() != null) {
            Long parentId = tempIds.get(itemMetadata.getParentId());
            if (parentId != null) {
               itemMetadata.setParentId(parentId);
            }
         }

         // if the itemMetadata does not have ID but has a TempID, maybe it was
         // set
         if (itemMetadata.getId() == null && itemMetadata.getTempId() != null) {
            Long newId = tempIds.get(itemMetadata.getTempId());
            if (newId != null) {
               itemMetadata.setId(newId);
            }
         }

         commitObject(itemMetadata, workspace, device);

         if (itemMetadata.getTempId() != null) {
            tempIds.put(itemMetadata.getTempId(), itemMetadata.getId());
         }

         objectResponse = itemMetadata;
         committed = true;

         responseObjects.add(new CommitInfo(itemMetadata.getVersion(), committed,
               objectResponse.toMetadataItem()));
      }

      return responseObjects;

   }

   private void saveNewVersion(ItemMetadataRMI metadata, ItemRMI serverItem,
         WorkspaceRMI workspace, DeviceRMI device) {

      // Create new objectVersion
      ItemVersionRMI itemVersion = new ItemVersionRMI(
            metadata.getId(),
            serverItem.getId(),
            device,
            metadata.getVersion(),
            Date.from(Instant.now()),
            metadata.getModifiedAt(),
            metadata.getChecksum(),
            metadata.getStatus(),
            metadata.getSize());

      add(itemVersion);

      // If no folder, create new chunks
      if (!metadata.isFolder()) {
         List<String> chunks = metadata.getChunks();
         createChunks(chunks, itemVersion);
      }

      // TODO To Test!!
      String status = metadata.getStatus();
      if (status.equals(Status.RENAMED.toString())
            || status.equals(Status.MOVED.toString())
            || status.equals(Status.DELETED.toString())) {

         serverItem.setFilename(metadata.getFilename());

         Long parentFileId = metadata.getParentId();
         if (parentFileId == null) {
            serverItem.setClientParentFileVersion(null);
            serverItem.setParent(null);
         } else {
            serverItem.setClientParentFileVersion(metadata
                  .getParentVersion());
            ItemRMI parent = findById(parentFileId);
            serverItem.setParent(parent);
         }
      }

      // Update object latest version
      serverItem.setLatestVersionNumber(metadata.getVersion());
      add(serverItem);

   }

   /*
    * Private functions
    */
   private void commitObject(ItemMetadataRMI itemMetadata, WorkspaceRMI workspace, DeviceRMI device)
         throws CommitWrongVersion, CommitWrongVersionNoParent, CommitExistantVersion {
      workspace.add(itemMetadata,device);
   }

   private void createChunks(List<String> chunksString, ItemVersionRMI objectVersion) {
      if (chunksString != null) {
         if (chunksString.size() > 0) {
            List<ChunkRMI> chunks = new ArrayList<>();
            int i = 0;
            for (String chunkName : chunksString) {
               chunks.add(new ChunkRMI(chunkName, i));
               i++;
            }
            insertChunks(objectVersion, chunks);
         }
      }
   }

}
