/**
 * 
 */
package com.stacksync.syncservice.dummy.server;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public abstract class AServerDummy extends Thread {

	protected final Logger logger = Logger.getLogger(AServerDummy.class.getName());

	protected static final int CHUNK_SIZE = 512 * 1024;

	protected int commitsPerMinute, minutes;
	protected ConnectionPool pool;
	protected Handler handler;

	public AServerDummy(ConnectionPool pool, int commitsPerMinute, int minutes) throws SQLException, NoStorageManagerAvailable {
		this.pool = pool;
		this.commitsPerMinute = commitsPerMinute;
		this.minutes = minutes;
		handler = new SQLSyncHandler(pool);
	}

	public Connection getConnection() {
		return handler.getConnection();
	}

	public void doCommit(UUID uuid, Random ran, int min, int max, String id) throws DAOException {
		// Create user info
		User user = new User();
		user.setId(uuid);
		Device device = new Device(uuid);
		Workspace workspace = new Workspace(uuid);

		// Create a ItemMetadata List
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(createItemMetadata(ran, min, max, uuid));

		logger.info("hander_doCommit_start,commitID=" + id);
		handler.doCommit(user, workspace, device, items);
		logger.info("hander_doCommit_end,commitID=" + id);
	}

	private ItemMetadata createItemMetadata(Random ran, int min, int max, UUID deviceId) {
		String[] mimes = { "pdf", "php", "java", "docx", "html", "png", "jpeg", "xml" };

		Long id = null;
		Long version = 1L;

		Long parentId = null;
		Long parentVersion = null;

		String status = "NEW";
		Date modifiedAt = new Date();
		Long checksum = (long) ran.nextInt(Integer.MAX_VALUE);
		List<String> chunks = new ArrayList<String>();
		Boolean isFolder = false;
		String filename = java.util.UUID.randomUUID().toString();
		String mimetype = mimes[ran.nextInt(mimes.length)];

		// Fill chunks
		int numChunks = ran.nextInt((max - min) + 1) + min;
		long size = numChunks * CHUNK_SIZE;
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

		ItemMetadata itemMetadata = new ItemMetadata(id, version, deviceId, parentId, parentVersion, status, modifiedAt, checksum, size,
				isFolder, filename, mimetype, chunks);
		itemMetadata.setChunks(chunks);
		itemMetadata.setTempId((long) ran.nextInt(10));

		return itemMetadata;
	}

	private String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		crypt.reset();
		crypt.update(str.getBytes("UTF-8"));

		return new BigInteger(1, crypt.digest()).toString(16);

	}

	public void createUser(UUID id) {
		try {
			String[] create = new String[] {
					"INSERT INTO user1 (id, name, swift_user, swift_account, email, quota_limit) VALUES ('" + id + "', '" + id + "', '"
							+ id + "', '" + id + "', '" + id + "@asdf.asdf', 0);",
					"INSERT INTO workspace (id, latest_revision, owner_id, is_shared, swift_container, swift_url) VALUES ('" + id
							+ "', 0, '" + id + "', false, '" + id + "', 'STORAGEURL');",
					"INSERT INTO workspace_user(workspace_id, user_id, workspace_name, parent_item_id) VALUES ('" + id + "', '" + id
							+ "', 'default', NULL);",
					"INSERT INTO device (id, name, user_id, os, app_version) VALUES ('" + id + "', '" + id + "', '" + id + "', 'LINUX', 1)" };

			Statement statement;

			statement = handler.getConnection().createStatement();

			for (String query : create) {
				statement.executeUpdate(query);
			}

			statement.close();
		} catch (SQLException e) {
			logger.error(e);
		}
	}
}