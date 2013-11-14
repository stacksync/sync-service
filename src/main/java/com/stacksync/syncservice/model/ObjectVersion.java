package com.stacksync.syncservice.model;

import java.util.Date;
import java.util.List;

public class ObjectVersion {

	private Long id;
	private Object1 object;
	private Device device;
	private Long version;
	private Date serverDateModified;
	private Long checksum;
	private Date clientDateModified;
	private String clientStatus;
	private Long clientFileSize;
	private String clientName;
	private String clientFilePath;
	private List<Chunk> chunks; // Just for bench

	public ObjectVersion() {
		this.id = null;
	}

	public ObjectVersion(Long id, Object1 object, Device device, Long version, Date serverDateModified, Long checksum,
			Date clientDateModified, String clientStatus, Long clientFileSize, String clientName, String clientFilePath) {
		this.id = id;
		this.object = object;
		this.device = device;
		this.version = version;
		this.serverDateModified = serverDateModified;
		this.checksum = checksum;
		this.clientDateModified = clientDateModified;
		this.clientStatus = clientStatus;
		this.clientFileSize = clientFileSize;
		this.clientName = clientName;
		this.clientFilePath = clientFilePath;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Object1 getObject() {
		return object;
	}

	public void setObject(Object1 object) {
		this.object = object;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Date getServerDateModified() {
		return serverDateModified;
	}

	public void setServerDateModified(Date serverDateModified) {
		this.serverDateModified = serverDateModified;
	}

	public Long getChecksum() {
		return checksum;
	}

	public void setChecksum(Long checksum) {
		this.checksum = checksum;
	}

	public Date getClientDateModified() {
		return clientDateModified;
	}

	public void setClientDateModified(Date clientDateModified) {
		this.clientDateModified = clientDateModified;
	}

	public String getClientStatus() {
		return clientStatus;
	}

	public void setClientStatus(String clientStatus) {
		this.clientStatus = clientStatus;
	}

	public Long getClientFileSize() {
		return clientFileSize;
	}

	public void setClientFileSize(Long clientFileSize) {
		this.clientFileSize = clientFileSize;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getClientFilePath() {
		return clientFilePath;
	}

	public void setClientFilePath(String clientFilePath) {
		this.clientFilePath = clientFilePath;
	}

	public List<Chunk> getChunks() {
		return chunks;
	}

	public void setChunks(List<Chunk> chunks) {
		this.chunks = chunks;
	}

	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String toString() {
		String format = "ObjectVersion[id=%s, objectId=%s, version=%s, chunks=%s, deviceId=%s, modified=%s, "
				+ "checksum=%s, clientModifiedTime=%s, clientStatus=%s, "
				+ "clientFileSize=%s, clientName=%s, clientPath=%s]";

		Long objectId = null;
		if (object != null) {
			objectId = object.getId();
		}

		Integer chunksSize = null;
		if (chunks != null) {
			chunksSize = chunks.size();
		}

		Long deviceId = null;
		if (device != null) {
			deviceId = device.getId();
		}

		String result = String.format(format, id, objectId, version, chunksSize, deviceId, serverDateModified,
				checksum, clientDateModified, clientStatus, clientFileSize, clientName, clientFilePath);

		return result;
	}
}
