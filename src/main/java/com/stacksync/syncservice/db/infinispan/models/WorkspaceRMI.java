package com.stacksync.syncservice.db.infinispan.models;

import com.stacksync.syncservice.db.Status;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
import org.infinispan.atomic.Distributed;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

@Distributed(key = "id")
public class WorkspaceRMI implements Serializable {

   public UUID id;

   private String name;
   private ItemRMI parentItem;
   private Integer latestRevision;
   private UserRMI owner;
   private String swiftContainer;
   private String swiftUrl;
   private boolean isShared;
   private boolean isEncrypted;
   private HashMap<Long, ItemRMI> items;
   private List<UserRMI> users;

   private long itemIdCounter, itemVersionIdCounter;
   private Random random = new Random();

   @Deprecated
   public WorkspaceRMI(){}

   public WorkspaceRMI(UUID id) {
      this(id, 0, null, false, false);
   }

   public WorkspaceRMI(UUID id, Integer latestRevision, UserRMI owner, boolean isShared, boolean isEncrypted) {
      this.id = id;
      this.latestRevision = latestRevision;
      this.owner = owner;
      this.isShared = isShared;
      this.isEncrypted = isEncrypted;
      this.items = new HashMap<>();
      this.users = new ArrayList<>();
      if (owner!=null)
         users.add(owner);
   }

   public UUID getId() {
      return id;
   }

   public void setId(UUID id) {
      this.id = id;
   }

   public Integer getLatestRevision() {
      return latestRevision;
   }

   public void setLatestRevision(Integer latestRevision) {
      this.latestRevision = latestRevision;
   }

   public UserRMI getOwner() {
      return owner;
   }

   public void setOwner(UserRMI owner) {
      this.owner = owner;
   }

   public boolean isShared() {
      return isShared;
   }

   public boolean isEncrypted() {
      return isEncrypted;
   }

   public void setEncrypted(Boolean isEncrypted) {
      this.isEncrypted = isEncrypted;
   }

   public void setShared(Boolean isShared) {
      this.isShared = isShared;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getSwiftContainer() {
      return swiftContainer;
   }

   public void setSwiftContainer(String swiftContainer) {
      this.swiftContainer = swiftContainer;
   }

   public String getSwiftUrl() {
      return swiftUrl;
   }

   public void setSwiftUrl(String swiftUrl) {
      this.swiftUrl = swiftUrl;
   }

   public ItemRMI getParentItem() {
      return parentItem;
   }

   public void setParentItem(ItemRMI parentItem) {
      this.parentItem = parentItem;
   }

   public HashMap<Long, ItemRMI> getItems() {
      return items;
   }

   public void setItems(HashMap<Long, ItemRMI> items) {
      this.items = items;
   }

   public void addItem(ItemRMI item) {
      this.items.put(item.getId(), item);
   }

   public List<UserRMI> getUsers() {
      return users;
   }

   public void setUsers(List<UserRMI> users) {
      this.users = users;
   }

   public void addUser(UserRMI user) {
      this.users.add(user);
   }

   public void removeUser(UUID user) {
      users.remove(user);
   }

   /**
    * Checks whether the user contains all required attributes (ID is not required since it is assigned automatically
    * when a user is inserted to the database)
    *
    * @return Boolean True if the user is valid. False otherwise.
    */
   public boolean isValid() {
      return this.owner != null;
   }

   @Override
   public String toString() {
      return String.format(
            "workspace[id=%s, latestRevision=%s, owner=%s, items=%s, users=%s]", id,
            latestRevision, owner, items, users);
   }

   public long addVersionRMI(ItemVersionRMI itemVersion) {
      ItemRMI item = items.get(itemVersion.getItemId());
      if (item != null) {
         itemVersion.setId(this.random.nextLong());
         item.addVersion(itemVersion);
         item.setLatestVersionNumber(itemVersion.getVersion());
      }
      return itemVersion.getId();
   }

   public void insertChunks(Long id, List chunks, Long itemVersionId) {
      ItemRMI item = items.get(id);
      List<ItemVersionRMI> versions;
      versions = item.getVersions();
      for (ItemVersionRMI version : versions) {
         if (version.getId().equals(itemVersionId)) {
            version.setChunks(chunks);
         }
      }
   }

   public ItemRMI finddById(Long id) {
      return items.get(id);
   }

   public List<ItemMetadataRMI> getItemsMetadata() {
      List<ItemMetadataRMI> ret = new ArrayList<>();
      for (ItemRMI item : getItems().values()){
         ret.add(ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item, item.getLatestVersion()));
      }
      return ret;
   }

   public ItemMetadataRMI getItemsMetadata(Boolean includeDeleted) {

      ItemMetadataRMI rootMetadata = new ItemMetadataRMI();
      rootMetadata.setIsFolder(true);
      rootMetadata.setFilename("root");
      rootMetadata.setIsRoot(true);
      rootMetadata.setVersion(new Long(0));

      for (ItemRMI item : items.values()) {
         ItemMetadataRMI itemMetadataRMI = ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item, item.getLatestVersion());
         if (itemMetadataRMI.getStatus().compareTo(Status.DELETED.toString()) == 0) {
            if (includeDeleted) {
               rootMetadata.addChild(itemMetadataRMI);
            }
         } else {
            rootMetadata.addChild(itemMetadataRMI);
         }
      }

      return rootMetadata;
   }

   public ItemMetadataRMI findById(Long id, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks){
      ItemMetadataRMI itemMetadata = null;
      ItemRMI item = items.get(id);
      if (item != null) {
         itemMetadata = getItemMetadataFromItem(item, version, includeList, includeDeleted, includeChunks);
      }
      return itemMetadata;
   }

   private ItemMetadataRMI getItemMetadataFromItem(ItemRMI item) {
      return getItemMetadataFromItem(item, item.getLatestVersionNumber(), false, false, false);
   }

   /**
    *
    * If version is <i>null</i> return the latest.
    *
    * @param item
    * @param version
    * @param includeList
    * @param includeDeleted
    * @param includeChunks
    * @return
    */
   private ItemMetadataRMI getItemMetadataFromItem(ItemRMI item, Long version, Boolean includeList,
         Boolean includeDeleted, Boolean includeChunks) {

      ItemMetadataRMI itemMetadata = null;

      if (includeList && item.isFolder()) {
         // Get children :D
         itemMetadata = addChildrenFromItemMetadata(itemMetadata, includeDeleted);
      }else{
         itemMetadata = item.getItemMetadataFromItem(version, includeChunks);
      }

      return itemMetadata;

   }

   private ItemMetadataRMI addChildrenFromItemMetadata(ItemMetadataRMI itemMetadata, Boolean includeDeleted) {
      for (ItemRMI thisItem : items.values()) {
         if (itemMetadata.getId().equals(thisItem.getParentId())
               && ((includeDeleted && itemMetadata.getStatus().equals("DELETED"))
               || !itemMetadata.getStatus().equals("DELETED"))) {
            ItemVersionRMI thisItemVersion = thisItem.getLatestVersion();
            ItemMetadataRMI child = ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(thisItem, thisItemVersion);
            itemMetadata.addChild(child);
         }
      }

      return itemMetadata;

   }

   public ItemRMI findById(Long id) {
      return items.get(id);
   }

   public void delete(Long id) {
      items.remove(id);
   }

   public ItemMetadataRMI findItemVersionsById(Long id) {

      ItemMetadataRMI itemMetadata = null;
      ItemRMI item = null;

      for (ItemRMI currentItem : items.values()) {
         if (currentItem.getId().equals(id)) {
            item = currentItem;
         }
      }

      if (item == null) {
         return null;
      }

      for (ItemVersionRMI itemVersion : item.getVersions()) {
         if (itemVersion.getVersion().equals(item.getLatestVersionNumber())) {
            itemMetadata = ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item, itemVersion);
            break;
         }
      }

      if (itemMetadata == null) {
         return null;
      }

      for (ItemVersionRMI itemVersion : item.getVersions()) {
         if (!itemVersion.getVersion().equals(item.getLatestVersionNumber())) {
            ItemMetadataRMI version = ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item, itemVersion);
            if (version != null) {
               itemMetadata.addChild(version);
            }
         }
      }
      return itemMetadata;
   }

   public void add(ItemVersionRMI itemVersion) {
      for (ItemRMI item : items.values()) {
         if (item.getId().equals(itemVersion.getItemId())) {
            itemVersion.setId(itemVersionIdCounter++);
            item.addVersion(itemVersion);
            item.setLatestVersionNumber(itemVersion.getVersion());
            break;
         }
      }
   }

   public void insertChunks(List<ChunkRMI> chunks, long itemVersionId) {
      List<ItemVersionRMI> versions;
      for (ItemRMI item : items.values()) {
         versions = item.getVersions();
         for (ItemVersionRMI version : versions) {
            if (version.getId().equals(itemVersionId)) {
               version.setChunks(chunks);
            }
         }
      }
   }

   public List<ChunkRMI> findChunks(Long itemVersionId) {
      List<ItemVersionRMI> versions;
      for (ItemRMI item : items.values()) {
         versions = item.getVersions();
         for (ItemVersionRMI version : versions) {
            if (version.getId().equals(itemVersionId)) {
               return version.getChunks();
            }
         }
      }
      return null;
   }

   public void update(ItemVersionRMI itemVersion){
      for (ItemRMI item : items.values()) {
         if (item.getId().equals(itemVersion.getItemId())) {
            List<ItemVersionRMI> versions = item.getVersions();
            for (ItemVersionRMI version : versions) {
               if (version.getVersion().equals(itemVersion.getVersion())) {
                  item.removeVersion(version);
                  item.addVersion(itemVersion);
                  break;
               }
            }
            break;
         }
      }
   }

   public void delete(ItemVersionRMI itemVersion) {
      for (ItemRMI item : items.values()) {
         if (item.getId().equals(itemVersion.getItemId())) {
            item.removeVersion(itemVersion);
            if (item.getLatestVersionNumber().equals(itemVersion.getVersion())) {
               item.setLatestVersionNumber(itemVersion.getVersion() - 1L);
            }
            break;
         }
      }
   }

   public boolean allow(UserRMI user) {
      return users.contains(user.getId());
   }

   public void add(ItemMetadataRMI metadata, DeviceRMI device)
         throws CommitWrongVersionNoParent, CommitWrongVersion, CommitExistantVersion {

      ItemRMI serverItem = items.get(id);

      if (serverItem == null) {

         // add a new item at version 1
         if (metadata.getVersion() == 1) {

            Long parentId = metadata.getParentId();
            ItemRMI parent = null;
            if (parentId != null) {
               parent = findById(parentId);
            }

            if (metadata.getStatus() == null)
               metadata.setStatus(Status.NEW.toString());

            // Insert item
            ItemRMI item = new ItemRMI(
                  metadata.getId(),
                  this,
                  metadata.getVersion(),
                  parent,
                  metadata.getFilename(),
                  metadata.getMimetype(),
                  metadata.isFolder(),
                  metadata.getParentVersion());
            items.putIfAbsent(item.getId(), item);

            // Insert version
            ItemVersionRMI objectVersion = new ItemVersionRMI(
                  random.nextLong(),
                  item.getId(),
                  device,
                  metadata.getVersion(),
                  metadata.getModifiedAt(),
                  metadata.getModifiedAt(),
                  metadata.getChecksum(),
                  metadata.getStatus(),
                  metadata.getSize());
            item.addVersion(objectVersion);

            // If not a folder, create appropriate new chunks
            if (!metadata.isFolder()) {
               objectVersion.createChunks(metadata.getChunks());
            }

         } else {

            throw new CommitWrongVersionNoParent("");

         }

      } else { // item already exists

         // Check if this version already exists in the server
         long serverVersion = serverItem.getLatestVersionNumber();
         long clientVersion = metadata.getVersion();
         boolean existVersionInServer = (serverVersion >= clientVersion);

         // if this exist, we check that they are the same
         if (existVersionInServer) {

            boolean lastVersion = (serverItem.getLatestVersion().equals(metadata.getVersion()));
            if (!lastVersion) {
               System.out.println("Item "+serverItem.getId()+" already exists in version "+metadata.getVersion());
               return;
            }

         } else {

            System.out.println(serverVersion +" VS "+clientVersion);
            // Ensure the version is correct and save it
            if (serverVersion + 1 == clientVersion) {

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
                  itemVersion.createChunks(metadata.getChunks());
               }

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

               // Set item latest version
               serverItem.setLatestVersionNumber(metadata.getVersion());

            } else {
               throw new CommitWrongVersion("Invalid version.", serverItem);
            }

         }

      }

   }

}
