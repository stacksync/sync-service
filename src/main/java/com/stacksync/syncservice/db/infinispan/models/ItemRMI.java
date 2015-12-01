package com.stacksync.syncservice.db.infinispan.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemRMI implements Serializable{

   public Long id;
   private Long latestVersion;
   private String filename;
   private String mimetype;
   private Long parentId;
   private Boolean isFolder;
   private Long clientParentFileVersion;

   private List<ItemVersionRMI> versions;

   private UUID workspaceId;

   @Deprecated
   public ItemRMI() {}

   public ItemRMI(Long id, UUID workspaceId, Long latestVersion, Long parentId,
         String filename, String mimetype, Boolean isFolder,
         Long clientParentFileVersion) {

      this.id = id;
      this.workspaceId= workspaceId;
      this.latestVersion = latestVersion;
      this.parentId = parentId;
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

   public UUID getWorkspaceId(){ return workspaceId;}

   public Long getParentId() {
      return parentId;
   }

   public void setParent(ItemRMI parent) {
      this.parentId = parent.getId();
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
      return parentId != null;
   }

   public boolean isValid() {
      return !(latestVersion == null || filename == null || mimetype == null || isFolder == null || versions == null);
   }

   @Override
   public String toString() {
      String format = "Item[id=%s, parentId=%s, latestVersion=%s, "
            + "Filename=%s, mimetype=%s, isFolder=%s, "
            + "clientParentFileVersion=%s, versions=%s]";

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

   @Override
   public int hashCode() {
      return id.hashCode();
   }
}