/**
 * 
 */
package com.stacksync.syncservice.dummy.actions;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public abstract class Action {
	protected static final double CHUNK_SIZE = 512 * 1024;

	protected Handler handler;
	protected UUID userId;
	protected Long fileId, fileSize, fileVersion;
	protected String status, fileType, fileMime;

	public Action(Handler handler, UUID userId, Long fileId, Long fileSize, String fileType, String fileMime, Long fileVersion) {
		this.handler = handler;
		this.userId = userId;
		this.fileId = fileId;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.fileMime = fileMime;
		this.fileVersion = fileVersion;
	}

	protected String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		crypt.reset();
		crypt.update(str.getBytes("UTF-8"));

		return new BigInteger(1, crypt.digest()).toString(16);

	}

	protected ItemMetadata createItemMetadata(Random ran) {

		Date modifiedAt = new Date();
		Long checksum = (long) ran.nextInt(Integer.MAX_VALUE);
		List<String> chunks = new ArrayList<String>();
		int numChunks = (int) Math.ceil(fileSize / CHUNK_SIZE);

		for (int i = 0; i < numChunks; i++) {
			String str = java.util.UUID.randomUUID().toString();
			try {
				chunks.add(doHash(str));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}

		ItemMetadata itemMetadata = new ItemMetadata(fileId, fileVersion, userId, null, null, status, modifiedAt, checksum, fileSize,
				false, fileId.toString(), fileMime, chunks);
		itemMetadata.setChunks(chunks);

		return itemMetadata;

	}

	public void doCommit() throws DAOException {
		User user = new User(userId);
		Device device = new Device(userId);
		Workspace workspace = new Workspace(userId);

		Random ran = new Random(System.currentTimeMillis());
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(createItemMetadata(ran));
		handler.doCommit(user, workspace, device, items);
	}
}
