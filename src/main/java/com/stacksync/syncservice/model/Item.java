package com.stacksync.syncservice.model;

import java.util.ArrayList;
import java.util.List;

public class Item {

	private Long id;
	private Workspace workspace;
	private Long latestVersion;
	private Item parent;
	private String filename;
	private String mimetype;
	private Boolean isFolder;
	private Long clientParentFileVersion;
	private List<ItemVersion> versions;

	public Item() {
		this(null);
	}

	public Item(Long id) {
		this(id, null, null, null, null, null, null, null, null);
	}

	public Item(Long id, Workspace workspace, Long latestVersion, Item parent, Long clientFileId,
			String filename, String mimetype, Boolean isFolder,
			Long clientParentFileVersion) {

		this.id = id;
		this.workspace = workspace;
		this.latestVersion = latestVersion;
		this.parent = parent;
		this.filename = filename;
		this.mimetype = mimetype;
		this.isFolder = isFolder;
		this.clientParentFileVersion = clientParentFileVersion;
		this.versions = new ArrayList<ItemVersion>();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	public Long getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(Long latestVersion) {
		this.latestVersion = latestVersion;
	}

	public Item getParent() {
		return parent;
	}
	
	public Long getParentId(){
		if (parent == null) return null;
		else return parent.getId();
	}

	public void setParent(Item parent) {
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

	public List<ItemVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<ItemVersion> versions) {
		this.versions = versions;
	}

	public void addVersion(ItemVersion objectVersion) {
		this.versions.add(objectVersion);
	}

	public void removeVersion(ItemVersion objectVersion) {
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

		if (workspace == null || latestVersion == null 
				|| filename == null || mimetype == null || isFolder == null || versions == null) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		String format = "Item[id=%s, parentId=%s, workspaceId=%s, latestVersion=%s, "
				+ "Filename=%s, "
				+ "mimetype=%s, isFolder=%s, "
				+ "clientParentFileVersion=%s, versions=%s]";

		Long parentId = null;
		if (parent != null) {
			parentId = parent.getId();
		}

		Long workspaceId = null;
		if (workspace != null) {
			workspaceId = workspace.getId();
		}

		Integer versionsSize = null;
		if (versions != null) {
			versionsSize = versions.size();
		}

		String result = String.format(format, id, parentId, workspaceId, latestVersion,
				filename, mimetype, isFolder,
				clientParentFileVersion, versionsSize);

		return result;
	}

}
