package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

import java.io.Serializable;
import java.util.*;

@Distributed
public class WorkspaceRMI implements Serializable {

   private static final long serialVersionUID = 243350300638953723L;

   @Key
   public UUID id;

   private String name;
   private ItemRMI parentItem;
   private Integer latestRevision;
   private UUID owner;
   private String swiftContainer;
   private String swiftUrl;
   private boolean isShared;
   private boolean isEncrypted;
   private HashMap<Long, ItemRMI> items;
   private List<UUID> users;
   private long itemIdCounter, itemVersionIdCounter;
   private Random random = new Random();

   public WorkspaceRMI() {
      this(UUID.randomUUID());
      this.itemIdCounter = 0;
      this.itemVersionIdCounter = 0;
   }

   public WorkspaceRMI(UUID id) {
      this(id, 0, null, false, false);
   }

   public WorkspaceRMI(UUID id, Integer latestRevision, UUID owner, boolean isShared, boolean isEncrypted) {
      this.id = id;
      this.latestRevision = latestRevision;
      this.owner = owner;
      this.isShared = isShared;
      this.isEncrypted = isEncrypted;
      this.items = new HashMap<>();
      this.users = new ArrayList<>();
      this.users.add(owner);
   }

   public void setWorkspace(WorkspaceRMI workspace) {
      this.id = workspace.getId();
      this.latestRevision = workspace.getLatestRevision();
      this.owner = workspace.getOwner();
      this.isShared = workspace.isShared();
      this.isEncrypted = workspace.isEncrypted();
      this.items = workspace.getItems();
      this.users = workspace.getUsers();
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

   public UUID getOwner() {
      return owner;
   }

   public void setOwner(UUID owner) {
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

   public List<UUID> getUsers() {
      return users;
   }

   public void setUsers(List<UUID> users) {
      this.users = users;
   }

   public void addUser(UUID user) {
      this.users.add(user);
   }

   public void removeUser(UUID user) {
      int index = 0;
      for (UUID id : users) {
         if (id.equals(user)) {
            users.remove(index);
            break;
         }
         index++;
      }
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
      List<ItemVersionRMI> versions = item.getVersions();
      if (version==null) {
         itemMetadata = ItemMetadataRMI
               .createItemMetadataFromItemAndItemVersion(item, versions.get(versions.size() - 1), includeChunks);
      } else {
         for (ItemVersionRMI itemVersion : versions) {
            if (itemVersion.getVersion().equals(version)) {
               itemMetadata = ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(item, itemVersion, includeChunks);
               break;
            }
         }
      }

      if (includeList && item.isFolder()) {
         // Get children :D
         itemMetadata = addChildrenFromItemMetadata(itemMetadata, includeDeleted);
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

   public void add(ItemRMI item) {
      items.put(item.getId(),item);
   }

   public void put(ItemRMI item) {
      boolean exist = items.containsKey(item.getId());
      if (!exist) {
         item.setId(this.itemIdCounter++);
         add(item);
      }
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

   public ItemMetadataRMI findByItemIdAndVersion(Long id, Long version) {
      return findById(id, Boolean.FALSE, version, Boolean.FALSE, Boolean.FALSE);
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
}
