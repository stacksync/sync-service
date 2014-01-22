package com.stacksync.syncservice.model;

import java.util.Date;
import java.util.List;

import com.stacksync.syncservice.models.ItemMetadata;

public class ItemVersion {

	private Long id;
	private Item item;
	private Device device;
	private Long version;
	private Date committedAt;
	private Date modifiedAt;
	private Long checksum;
	private String status;
	private Long size;
	private List<Chunk> chunks;

	public ItemVersion() {
		this.id = null;
	}

	public ItemVersion(Long id, Item item, Device device, Long version, Date committedAt,
			Date modifiedAt, Long checksum, String status, Long size) {
		this.id = id;
		this.item = item;
		this.device = device;
		this.version = version;
		this.committedAt = committedAt;
		this.modifiedAt = modifiedAt;
		this.checksum = checksum;
		this.status = status;
		this.size = size;
	}
	
	public ItemVersion(ItemMetadata metadata) {
		this.id = metadata.getVersion();
		this.item = new Item(metadata.getId());
		this.device = new Device(metadata.getDeviceId());
		this.version = metadata.getVersion();
		this.modifiedAt = metadata.getModifiedAt();
		this.checksum = metadata.getChecksum();
		this.status = metadata.getStatus();
		this.size = metadata.getSize();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
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

	public Date getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Date modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public Long getChecksum() {
		return checksum;
	}

	public void setChecksum(Long checksum) {
		this.checksum = checksum;
	}

	public Date getCommittedAt() {
		return committedAt;
	}

	public void setCommittedAt(Date committedAt) {
		this.committedAt = committedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
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
		String format = "ItemVersion[id=%s, itemId=%s, version=%s, chunks=%s, deviceId=%s, modifiedAt=%s, "
				+ "committedAt=%s, checksum=%s, status=%s, "
				+ "size=%s]";

		Long itemId = null;
		if (item != null) {
			itemId = item.getId();
		}

		Integer chunksSize = null;
		if (chunks != null) {
			chunksSize = chunks.size();
		}

		Long deviceId = null;
		if (device != null) {
			deviceId = device.getId();
		}

		String result = String.format(format, id, itemId, version, chunksSize, deviceId, modifiedAt,
				committedAt, checksum, status, size);

		return result;
	}
}
