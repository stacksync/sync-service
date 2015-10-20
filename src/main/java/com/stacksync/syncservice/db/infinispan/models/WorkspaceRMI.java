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
      this(null);
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
      this.items = new HashMap<Long, ItemRMI>();
      this.users = new ArrayList<UUID>();
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

   //*******InfinispanDAO*******
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
   //*******InfinispanDAO*******
   //*******InfinispanDAO*******
    /*
     public ItemMetadata findById(Long id, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks) throws RemoteException {

     ItemMetadata itemMetadata = null;
     ItemRMI item = items.get(id);
     if (item != null) {
     itemMetadata = getItemMetadataFromItem(item, version, includeList, includeDeleted, includeChunks);
     }
     return itemMetadata;
     }

     private ItemMetadata getItemMetadataFromItem(ItemRMI item) {

     return getItemMetadataFromItem(item, item.getLatestVersionNumber(), false, false, false);

     }

     private ItemMetadata getItemMetadataFromItem(ItemRMI item, Long version, Boolean includeList, Boolean includeDeleted, Boolean includeChunks) {

     ItemMetadata itemMetadata = null;

     List<ItemVersionRMI> versions = item.getVersions();
     for (ItemVersionRMI itemVersion : versions) {
     if (itemVersion.getVersion().equals(version)) {
     itemMetadata = createItemMetadataFromItemAndItemVersion(item, itemVersion, includeChunks);
     if (includeList && item.isFolder()) {
     // Get children :D
     itemMetadata = addChildrenFromItemMetadata(itemMetadata, includeDeleted);
     }
     break;
     }
     }

     return itemMetadata;

     }

     private ItemMetadata addChildrenFromItemMetadata(ItemMetadata itemMetadata, Boolean includeDeleted) {

        
     for (ItemRMI thisItem : items) {
     if (itemMetadata.getId().equals(thisItem.getParentId()) && ((includeDeleted && itemMetadata.getStatus().equals("DELETED")) || !itemMetadata.getStatus().equals("DELETED"))) {
     ItemVersionRMI thisItemVersion = thisItem.getLatestVersion();
     ItemMetadata child = createItemMetadataFromItemAndItemVersion(thisItem, thisItemVersion);
     itemMetadata.addChild(child);
     }
     }

     return itemMetadata;

     }

     private ItemMetadata createItemMetadataFromItemAndItemVersion(ItemRMI item, ItemVersionRMI itemVersion) {

     return createItemMetadataFromItemAndItemVersion(item, itemVersion, false);

     }

     private ItemMetadata createItemMetadataFromItemAndItemVersion(ItemRMI item, ItemVersionRMI itemVersion, Boolean includeChunks) {

     ArrayList<String> chunks = new ArrayList<String>();
     if (includeChunks) {
     for (ChunkRMI chunk : itemVersion.getChunks()) {
     chunks.add(chunk.toString());
     }
     } else {
     chunks = null;
     }

     return new ItemMetadata(item.getId(), itemVersion.getVersion(), itemVersion.getDevice().getId(), item.getParentId(), item.getClientParentFileVersion(), itemVersion.getStatus(), itemVersion.getModifiedAt(), itemVersion.getChecksum(), itemVersion.getSize(), item.isFolder(), item.getFilename(), item.getMimetype(), chunks);

     }

     //************************************
     //************************************
     //*************** ITEM ***************
     //************************************
     //************************************
     public ItemRMI findById(Long id) throws RemoteException {

     for (ItemRMI i : items) {
     if (i.getId().equals(id)) {
     return i;
     }
     }
     return null;
     }

     public void add(ItemRMI item) throws RemoteException {

     items.add(item);
     }

     public void update(ItemRMI item) throws RemoteException {

     for (ItemRMI i : items) {
     if (i.getId().equals(item.getId())) {
     items.remove(i);
     items.add(item);
     break;
     }
     }
     }

     public void put(ItemRMI item) throws RemoteException {

     boolean exist = false;

     for (ItemRMI i : items) {
     if (i.getId().equals(item.getId())) {
     update(item);
     exist = true;
     break;
     }
     }

     if (!exist) {
     item.setId(this.itemIdCounter++);
     add(item);
     }
     }

     public void delete(Long id) throws RemoteException {

     for (ItemRMI i : items) {
     if (i.getId().equals(id)) {
     items.remove(i);
     break;
     }
     }
     }

     public List<ItemMetadata> getItemsById(Long id) throws RemoteException {

     List<ItemMetadata> result = new ArrayList<ItemMetadata>();
     ItemMetadata itemMetadata;

     for (ItemRMI item : items) {
     if (item.getId().equals(id) || (item.getParentId() != null && item.getParentId().equals(id))) {
     itemMetadata = getItemMetadataFromItem(item);
     if (itemMetadata != null) {
     result.add(itemMetadata);
     }
     }
     }

     return result;

     }

     public ItemMetadata findItemVersionsById(Long id) throws RemoteException {

     ItemMetadata itemMetadata = null;
     ItemRMI item = null;

     for (ItemRMI currentItem : items) {
     if (currentItem.getId().equals(id)) {
     item = currentItem;
     }
     }

     if (item == null) {
     return null;
     }

     for (ItemVersionRMI itemVersion : item.getVersions()) {
     if (itemVersion.getVersion().equals(item.getLatestVersionNumber())) {
     itemMetadata = createItemMetadataFromItemAndItemVersion(item, itemVersion);
     break;
     }
     }

     if (itemMetadata == null) {
     return null;
     }

     for (ItemVersionRMI itemVersion : item.getVersions()) {
     if (!itemVersion.getVersion().equals(item.getLatestVersionNumber())) {
     ItemMetadata version = createItemMetadataFromItemAndItemVersion(item, itemVersion);
     if (version != null) {
     itemMetadata.addChild(version);
     }
     }
     }
     return itemMetadata;
     }

     //************************************
     //************************************
     //************ ITEMVERSION ***********
     //************************************
     //************************************
     public ItemMetadata findByItemIdAndVersion(Long id, Long version) throws RemoteException {

     return findById(id, Boolean.FALSE, version, Boolean.FALSE, Boolean.FALSE);

     }

     public void add(ItemVersionRMI itemVersion) throws RemoteException {

     for (ItemRMI item : items) {
     if (item.getId().equals(itemVersion.getItemId())) {
     itemVersion.setId(itemVersionIdCounter++);
     item.addVersion(itemVersion);
     item.setLatestVersionNumber(itemVersion.getVersion());
     break;
     }
     }
     }

     public void insertChunks(List<ChunkRMI> chunks, long itemVersionId) throws RemoteException {

     List<ItemVersionRMI> versions;

     for (ItemRMI item : items) {
     versions = item.getVersions();
     for (ItemVersionRMI version : versions) {
     if (version.getId().equals(itemVersionId)) {
     version.setChunks(chunks);
     }
     }
     }
     }

     public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException {

     List<ItemVersionRMI> versions;

     for (ItemRMI item : items) {
     versions = item.getVersions();
     for (ItemVersionRMI version : versions) {
     if (version.getId().equals(itemVersionId)) {
     return version.getChunks();
     }
     }
     }
     return null;
     }

     public void update(ItemVersionRMI itemVersion) throws RemoteException {

     for (ItemRMI item : items) {
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

     public void delete(ItemVersionRMI itemVersion) throws RemoteException {

     for (ItemRMI item : items) {
     if (item.getId().equals(itemVersion.getItemId())) {
     item.removeVersion(itemVersion);
     if (item.getLatestVersionNumber().equals(itemVersion.getVersion())) {
     item.setLatestVersionNumber(itemVersion.getVersion() - 1L);
     }
     break;
     }
     }
     }*/
}
