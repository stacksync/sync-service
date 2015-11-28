package com.stacksync.syncservice.db.infinispan.models;

import org.infinispan.atomic.Distributed;

import java.util.ArrayList;
import java.util.List;

@Distributed(key = "id")
public class ItemRMI {

   public Long id;
   private Long latestVersion;
   private ItemRMI parent;
   private String filename;
   private String mimetype;
   private Boolean isFolder;
   private Long clientParentFileVersion;

   private List<ItemVersionRMI> versions;

   private WorkspaceRMI workspaceRMI;

   @Deprecated
   public ItemRMI() {}

   public ItemRMI(Long id, WorkspaceRMI workspace, Long latestVersion, ItemRMI parent,
         String filename, String mimetype, Boolean isFolder,
         Long clientParentFileVersion) {

      this.id = id;
      this.workspaceRMI = workspace;
      this.latestVersion = latestVersion;
      this.parent = parent;
      this.filename = filename;
      this.mimetype = mimetype;
      this.isFolder = isFolder;
      this.clientParentFileVersion = clientParentFileVersion;
      this.versions = new ArrayList<>();
   }

   public ItemRMI(Long parentItemId) {
      this.id = parentItemId;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getLatestVersionNumber() {
      return latestVersion;
   }

   public void setLatestVersionNumber(Long latestVersion) {
      this.latestVersion = latestVersion;
   }

   public ItemVersionRMI getLatestVersion() {
      for (ItemVersionRMI version : versions) {
         if (version.getVersion().equals(latestVersion)) {
            return version;
         }
      }
      return null;
   }

   public ItemRMI getParent() {
      return parent;
   }

   public WorkspaceRMI getWorkspace(){ return workspaceRMI;}

   public Long getParentId() {
      if (parent == null) {
         return null;
      } else {
         return parent.getId();
      }
   }

   public void setParent(ItemRMI parent) {
      this.parent = parent;
   }

   public String getFilename() {
      return filename;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public String getMimetype() {
      return mimetype;
   }

   public void setMimetype(String mimetype) {
      this.mimetype = mimetype;
   }

   public Boolean isFolder() {
      return isFolder;
   }

   public void setIsFolder(Boolean isFolder) {
      this.isFolder = isFolder;
   }

   public Long getClientParentFileVersion() {
      return clientParentFileVersion;
   }

   public void setClientParentFileVersion(Long clientParentFileVersion) {
      this.clientParentFileVersion = clientParentFileVersion;
   }

   public List<ItemVersionRMI> getVersions() {
      return versions;
   }

   public void setVersions(List<ItemVersionRMI> versions) {
      this.versions = versions;
   }

   public void addVersion(ItemVersionRMI version) {
      assert version!=null;
      if (!versions.contains(version)) {
         versions.add(version);
         if (version.getVersion() > latestVersion)
            latestVersion = version.getVersion();
      }
   }

   public void removeVersion(ItemVersionRMI objectVersion) {
      this.versions.remove(objectVersion);
   }

   public boolean hasParent() {

      boolean has = true;
      if (this.parent == null) {
         has = false;
      }
      return has;
   }

   public boolean isValid() {
      return !(latestVersion == null || filename == null || mimetype == null || isFolder == null || versions == null);
   }

   @Override
   public String toString() {
      String format = "Item[id=%s, parentId=%s, latestVersion=%s, "
            + "Filename=%s, mimetype=%s, isFolder=%s, "
            + "clientParentFileVersion=%s, versions=%s]";

      Long parentId = null;
      if (parent != null) {
         parentId = parent.getId();
      }

      Integer versionsSize = null;
      if (versions != null) {
         versionsSize = versions.size();
      }

      String result = String.format(format, id, parentId, latestVersion,
            filename, mimetype, isFolder,
            clientParentFileVersion, versionsSize);

      return result;
   }

   public ItemVersionRMI getVersion(long version) {
      for (ItemVersionRMI itemVersion : versions) {
         if (itemVersion.getVersion().equals(version))
            return itemVersion;
      }
      return null;
   }

   public ItemMetadataRMI getItemMetadataFromItem(Long version, Boolean includeChunks) {
      ItemMetadataRMI itemMetadata = null;
      if (version==null) {
         itemMetadata = ItemMetadataRMI
               .createItemMetadataFromItemAndItemVersion(this, versions.get(versions.size() - 1), includeChunks);
      } else {
         for (ItemVersionRMI itemVersion : versions) {
            if (itemVersion.getVersion().equals(version)) {
               itemMetadata = ItemMetadataRMI.createItemMetadataFromItemAndItemVersion(this, itemVersion, includeChunks);
               break;
            }
         }
      }
      System.out.println(version +" vs "+versions);
      assert itemMetadata!=null;
      return itemMetadata;

   }

}