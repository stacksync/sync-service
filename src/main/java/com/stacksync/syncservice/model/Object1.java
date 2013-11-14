package com.stacksync.syncservice.model;

import java.util.ArrayList;
import java.util.List;

public class Object1 {

	private Long id;
	private String rootId;
	private Workspace workspace;
	private Long latestVersion;
	private Object1 parent;
	private Long clientFileId;
	private String clientFileName;
	private String clientFileMimetype;
	private Boolean clientFolder;
	private String clientParentRootId;
	private Long clientParentFileId;
	private Long clientParentFileVersion;
	private List<ObjectVersion> versions;

	public Object1() {
		this(null);
	}

	public Object1(Long id) {
		this(id, null, null, null, null, null, null, null, null, null, null, null);
	}

	public Object1(Long id, String rootId, Workspace workspace, Long latestVersion, Object1 parent, Long clientFileId,
			String clientFileName, String clientFileMimetype, Boolean clientFolder, String clientParentRootId,
			Long clientParentFileId, Long clientParentFileVersion) {

		this.id = id;
		this.rootId = rootId;
		this.workspace = workspace;
		this.latestVersion = latestVersion;
		this.parent = parent;
		this.clientFileId = clientFileId;
		this.clientFileName = clientFileName;
		this.clientFileMimetype = clientFileMimetype;
		this.clientFolder = clientFolder;
		this.clientParentRootId = clientParentRootId;
		this.clientParentFileId = clientParentFileId;
		this.clientParentFileVersion = clientParentFileVersion;
		this.versions = new ArrayList<ObjectVersion>();
	}

	public String getRootId() {
		return rootId;
	}

	public void setRootId(String rootId) {
		this.rootId = rootId;
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

	public Object1 getParent() {
		return parent;
	}

	public void setParent(Object1 parent) {
		this.parent = parent;
	}

	public Long getClientFileId() {
		return clientFileId;
	}

	public void setClientFileId(Long clientFileId) {
		this.clientFileId = clientFileId;
	}

	public String getClientFileName() {
		return clientFileName;
	}

	public void setClientFileName(String clientFileName) {
		this.clientFileName = clientFileName;
	}

	public String getClientFileMimetype() {
		return clientFileMimetype;
	}

	public void setClientFileMimetype(String clientFileMimetype) {
		this.clientFileMimetype = clientFileMimetype;
	}

	public Boolean getClientFolder() {
		return clientFolder;
	}

	public void setClientFolder(Boolean clientFolder) {
		this.clientFolder = clientFolder;
	}

	public String getClientParentRootId() {
		return clientParentRootId;
	}

	public void setClientParentRootId(String clientParentRootId) {
		this.clientParentRootId = clientParentRootId;
	}

	public Long getClientParentFileId() {
		return clientParentFileId;
	}

	public void setClientParentFileId(Long clientParentFileId) {
		this.clientParentFileId = clientParentFileId;
	}

	public Long getClientParentFileVersion() {
		return clientParentFileVersion;
	}

	public void setClientParentFileVersion(Long clientParentFileVersion) {
		this.clientParentFileVersion = clientParentFileVersion;
	}

	public List<ObjectVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<ObjectVersion> versions) {
		this.versions = versions;
	}

	public void addVersion(ObjectVersion objectVersion) {
		this.versions.add(objectVersion);
	}

	public void removeVersion(ObjectVersion objectVersion) {
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
	 * Returns True if the {@link Object1} is valid. False otherwise.
	 * 
	 * @return Boolean indicating whether the {@link Object1} is valid or not.
	 */
	public boolean isValid() {

		if (rootId == null || workspace == null || latestVersion == null || clientFileId == null
				|| clientFileName == null || clientFileMimetype == null || clientFolder == null || versions == null) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		String format = "Object1[id=%s, parentId=%s, workspaceId=%s, latestVersion=%s, "
				+ "rootId=%s, clientFileId=%s, clientFileName=%s, "
				+ "clientFileMimetype=%s, clientFolder=%s, clientParentRootId=%s, "
				+ "clientParentFileId=%s, clientParentFileVersion=%s, versions=%s]";

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

		String result = String.format(format, id, parentId, workspaceId, latestVersion, rootId, clientFileId,
				clientFileName, clientFileMimetype, clientFolder, clientParentRootId, clientParentFileId,
				clientParentFileVersion, versionsSize);

		return result;
	}

}
