package com.stacksync.syncservice.db.infinispan.models;

import com.stacksync.commons.models.ItemMetadata;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ItemRMI implements Serializable {

    private static final long serialVersionUID = 1482457936400001556L;

    private Long id;
    private Long latestVersion;
    private ItemRMI parent;
    private String filename;
    private String mimetype;
    private Boolean isFolder;
    private Long clientParentFileVersion;
    private List<ItemVersionRMI> versions;

    public ItemRMI() {
        this(null);
    }

    public ItemRMI(Long id) {
        this(id, null, null, null, null, null, null, null, null);
    }

    public ItemRMI(Long id, WorkspaceRMI workspace, Long latestVersion, ItemRMI parent, Long clientFileId,
            String filename, String mimetype, Boolean isFolder,
            Long clientParentFileVersion) {

        this.id = id;
        this.latestVersion = latestVersion;
        this.parent = parent;
        this.filename = filename;
        this.mimetype = mimetype;
        this.isFolder = isFolder;
        this.clientParentFileVersion = clientParentFileVersion;
        this.versions = new ArrayList<ItemVersionRMI>();
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

    public void addVersion(ItemVersionRMI objectVersion) {
        this.versions.add(objectVersion);
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

    /**
     * Returns True if the {@link Item} is valid. False otherwise.
     *
     * @return Boolean indicating whether the {@link Item} is valid or not.
     */
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
    
}
