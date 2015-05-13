/**
 * 
 */
package com.stacksync.syncservice.dummy;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
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
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class StaticBenchmark extends Thread {

	protected final Logger logger = Logger.getLogger(StaticBenchmark.class.getName());

	protected static final int CHUNK_SIZE = 512 * 1024;

	protected int commitsPerSecond, minutes, itemsCount;
	private UserContainer[] uuids;
	// private UUID[] uuids;
	protected ConnectionPool pool;
	protected Handler handler;

	public StaticBenchmark(ConnectionPool pool, int numUsers, int commitsPerSecond, int minutes) throws SQLException,
			NoStorageManagerAvailable {
		this.pool = pool;
		this.commitsPerSecond = commitsPerSecond;
		this.minutes = minutes;
		handler = new SQLSyncHandler(pool);
		itemsCount = 0;

		uuids = new UserContainer[numUsers];
		for (int i = 0; i < numUsers; i++) {
			createUser(uuids, i);
		}
	}

	public Connection getConnection() {
		return handler.getConnection();
	}

	@Override
	public void run() {
		Random ran = new Random(System.currentTimeMillis());

		// Distance between commits in msecs
		long distance = (long) (1000 / commitsPerSecond);

		// Every iteration takes a minute
		for (int i = 0; i < minutes; i++) {

			long startMinute = System.currentTimeMillis();
			for (int j = 0; j < commitsPerSecond * 60; j++) {
				String id = UUID.randomUUID().toString();

				// logger.info("serverDummy2_doCommit_start,commitID=" + id);
				long start = System.currentTimeMillis();
				try {
					doCommit(uuids[ran.nextInt(uuids.length)], ran, 1, 8, id);
					itemsCount++;
				} catch (DAOException e1) {
					logger.error(e1);
				}
				long end = System.currentTimeMillis();
				// logger.info("serverDummy2_doCommit_end,commitID=" + id);

				// If doCommit had no cost sleep would be distance but we have
				// to take into account of the time that it takes
				long sleep = distance - (end - start);
				if (sleep > 0) {
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			long endMinute = System.currentTimeMillis();
			long minute = endMinute - startMinute;

			// I will forgive 5 seconds of delay...
			if (minute > 65 * 1000) {
				// Notify error
				logger.error("MORE THAN 65 SECONDS=" + (minute / 1000));
			}
		}

	}

	public void doCommit(UserContainer container, Random ran, int min, int max, String id) throws DAOException {
		// Create user info
		User user = container.getUser();
		Device device = container.getDevice();
		Workspace workspace = container.getWorkspace();

		// Create a ItemMetadata List
		List<ItemMetadata> items = new ArrayList<ItemMetadata>();
		items.add(createItemMetadata(ran, min, max, device.getId()));

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

	public void createUser(UserContainer[] list, int i) {
		try {
			UUID id = UUID.randomUUID();
			String idStr = id.toString();
			User user = new User();
			user = new User();
			user.setName(idStr);
			user.setId(id);
			user.setEmail(idStr);
			user.setSwiftUser(idStr);
			user.setSwiftAccount(idStr);
			user.setQuotaLimit(2048);
			user.setQuotaUsed(1403);

			UserDAO uDao = handler.getUserDao();
			uDao.add(user);

			Workspace workspace = new Workspace();
			workspace.setEncrypted(false);
			workspace.setOwner(user);
			workspace.setShared(false);
			workspace.setName(idStr);
			workspace.setLatestRevision(0);

			WorkspaceDAO wDao = handler.getWorkspaceDAO();
			wDao.add(workspace);

			Device device = new Device(id, idStr, user);
			device.setAppVersion("0");

			DeviceDAO dDao = handler.getDeviceDao();
			dDao.add(device);

			list[i] = new UserContainer(user, workspace, device);

		} catch (Exception e) {
			logger.error(e);
		}
	}
}
