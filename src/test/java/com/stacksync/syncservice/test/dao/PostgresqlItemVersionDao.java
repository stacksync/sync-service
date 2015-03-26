/**
 * 
 */
package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler.Status;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class PostgresqlItemVersionDao {

	private static final int CHUNK_SIZE = 512 * 1024;
	private static Connection connection;
	private static User user;
	private static UserDAO userDao;
	private static Workspace workspace;
	private static WorkspaceDAO workspaceDao;
	private static Device device;
	private static DeviceDAO deviceDao;
	private static Item item;
	private static ItemDAO itemDao;
	private static ItemVersionDAO itemVersionDao;
	private static SecureRandom random;

	@BeforeClass
	public static void testSetup() throws ClassNotFoundException, SQLException, DAOException {
		random = new SecureRandom();

		String HOST = "10.30.233.113";
		int PORT = 5432;
		String DB = "testdb";
		String USER = "testos";
		String PASS = "testos";

		Class.forName("org.postgresql.Driver");
		connection = DriverManager.getConnection("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB, USER, PASS);

		String dataSource = "postgresql";
		DAOFactory factory = new DAOFactory(dataSource);

		userDao = factory.getUserDao(connection);
		workspaceDao = factory.getWorkspaceDao(connection);
		deviceDao = factory.getDeviceDAO(connection);

		// Create a user for the workspace
		user = new User();
		user.setName(nextString());
		user.setId(UUID.randomUUID());
		user.setEmail(nextString());
		user.setSwiftUser(nextString());
		user.setSwiftAccount(nextString());
		user.setQuotaLimit(2048);
		user.setQuotaUsed(1403);

		userDao.add(user);

		workspace = new Workspace();
		workspace.setLatestRevision(0);
		workspace.setOwner(user);
		workspaceDao.add(workspace);

		device = new Device();
		device.setAppVersion("1");
		device.setLastIp("192.168.1.1");
		device.setName("1+1");
		device.setOs("Android");
		device.setUser(user);

		deviceDao.add(device);

		itemDao = factory.getItemDAO(connection);
		itemVersionDao = factory.getItemVersionDAO(connection);
	}

	@AfterClass
	public static void testEnd() throws DAOException, SQLException {
		userDao.delete(user.getId()); // This should remove everything else...
		connection.close();
	}

	@Test
	public void test() throws CommitWrongVersionNoParent, CommitWrongVersion, CommitExistantVersion, DAOException {
		Random ran = new Random(System.currentTimeMillis());
		ItemMetadata item = createItemMetadata(ran, 4, 8);

		commitObject(user, item, workspace, device);

		ItemMetadata actual = itemVersionDao.findByItemIdAndVersion(user.getId(), item.getId(), item.getVersion());
		assertEquals(item.getId(), actual.getId());

		List<ItemMetadata> list = itemDao.getItemsByWorkspaceId(user.getId(), workspace.getId());
		for (ItemMetadata i : list) {
			if (item.getId().equals(i.getId())) {
				return;
			}
		}
		assertTrue(false);
	}

	// Handler functions
	private void commitObject(User user, ItemMetadata item, Workspace workspace, Device device) throws CommitWrongVersionNoParent,
			CommitWrongVersion, CommitExistantVersion, DAOException {

		Item serverItem = itemDao.findById(user.getId(), item.getId());

		// Check if this object already exists in the server.
		if (serverItem == null) {
			if (item.getVersion() == 1) {
				this.saveNewObject(user, item, workspace, device);
			} else {
				throw new CommitWrongVersionNoParent();
			}
			return;
		}

		// Check if the client version already exists in the server
		long serverVersion = serverItem.getLatestVersion();
		long clientVersion = item.getVersion();
		boolean existVersionInServer = (serverVersion >= clientVersion);

		if (existVersionInServer) {
			this.saveExistentVersion(user, serverItem, item);
		} else {
			// Check if version is correct
			if (serverVersion + 1 == clientVersion) {
				this.saveNewVersion(user, item, serverItem, workspace, device);
			} else {
				throw new CommitWrongVersion("Invalid version.", serverItem);
			}
		}
	}

	private void saveNewVersion(User user, ItemMetadata metadata, Item serverItem, Workspace workspace, Device device) throws DAOException {

		try {
			// Create new objectVersion
			ItemVersion itemVersion = new ItemVersion();
			itemVersion.setVersion(metadata.getVersion());
			itemVersion.setModifiedAt(metadata.getModifiedAt());
			itemVersion.setChecksum(metadata.getChecksum());
			itemVersion.setStatus(metadata.getStatus());
			itemVersion.setSize(metadata.getSize());

			itemVersion.setItem(serverItem);
			itemVersion.setDevice(device);

			itemVersionDao.add(user, itemVersion);

			// If no folder, create new chunks
			if (!metadata.isFolder()) {
				List<String> chunks = metadata.getChunks();
				this.createChunks(user, chunks, itemVersion);
			}

			// TODO To Test!!
			String status = metadata.getStatus();
			if (status.equals(Status.RENAMED.toString()) || status.equals(Status.MOVED.toString())
					|| status.equals(Status.DELETED.toString())) {

				serverItem.setFilename(metadata.getFilename());

				Long parentFileId = metadata.getParentId();
				if (parentFileId == null) {
					serverItem.setClientParentFileVersion(null);
					serverItem.setParent(null);
				} else {
					serverItem.setClientParentFileVersion(metadata.getParentVersion());
					Item parent = itemDao.findById(user.getId(), parentFileId);
					serverItem.setParent(parent);
				}
			}

			// Update object latest version
			serverItem.setLatestVersion(metadata.getVersion());
			itemDao.put(user, serverItem);

		} catch (Exception e) {
			throw new DAOException(e);
		}
	}

	private void saveNewObject(User user, ItemMetadata metadata, Workspace workspace, Device device) throws DAOException {
		// Create workspace and parent instances
		Long parentId = metadata.getParentId();
		Item parent = null;
		if (parentId != null) {
			parent = itemDao.findById(user.getId(), parentId);
		}

		try {
			// Insert object to DB

			item = new Item();
			item.setId(metadata.getId());
			item.setFilename(metadata.getFilename());
			item.setMimetype(metadata.getMimetype());
			item.setIsFolder(metadata.isFolder());
			item.setClientParentFileVersion(metadata.getParentVersion());

			item.setLatestVersion(metadata.getVersion());
			item.setWorkspace(workspace);
			item.setParent(parent);

			itemDao.put(user, item);

			// set the global ID
			metadata.setId(item.getId());

			// Insert objectVersion
			ItemVersion objectVersion = new ItemVersion();
			objectVersion.setVersion(metadata.getVersion());
			objectVersion.setModifiedAt(metadata.getModifiedAt());
			objectVersion.setChecksum(metadata.getChecksum());
			objectVersion.setStatus(metadata.getStatus());
			objectVersion.setSize(metadata.getSize());

			objectVersion.setItem(item);
			objectVersion.setDevice(device);
			itemVersionDao.add(user, objectVersion);

			// If no folder, create new chunks
			if (!metadata.isFolder()) {
				List<String> chunks = metadata.getChunks();
				this.createChunks(user, chunks, objectVersion);
			}
		} catch (Exception e) {
			throw new DAOException(e);
		}
	}

	private void saveExistentVersion(User user, Item serverObject, ItemMetadata clientMetadata) throws CommitWrongVersion,
			CommitExistantVersion, DAOException {

		ItemMetadata serverMetadata = this.getServerObjectVersion(user, serverObject, clientMetadata.getVersion());

		if (!clientMetadata.equals(serverMetadata)) {
			throw new CommitWrongVersion("Invalid version.", serverObject);
		}

		boolean lastVersion = (serverObject.getLatestVersion().equals(clientMetadata.getVersion()));

		if (!lastVersion) {
			throw new CommitExistantVersion("This version already exists.", serverObject, clientMetadata.getVersion());
		}
	}

	private ItemMetadata getServerObjectVersion(User user, Item serverObject, long requestedVersion) throws DAOException {

		ItemMetadata metadata = itemVersionDao.findByItemIdAndVersion(user.getId(), serverObject.getId(), requestedVersion);

		return metadata;
	}

	private void createChunks(User user, List<String> chunksString, ItemVersion objectVersion) throws IllegalArgumentException,
			DAOException {
		if (chunksString != null) {
			if (chunksString.size() > 0) {
				List<Chunk> chunks = new ArrayList<Chunk>();
				int i = 0;

				for (String chunkName : chunksString) {
					chunks.add(new Chunk(chunkName, i));
					i++;
				}

				itemVersionDao.insertChunks(user, chunks, objectVersion.getId());
			}
		}
	}

	public static String nextString() {
		return new BigInteger(130, random).toString(32);
	}

	private String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		crypt.reset();
		crypt.update(str.getBytes("UTF-8"));

		return new BigInteger(1, crypt.digest()).toString(16);
	}

	private ItemMetadata createItemMetadata(Random ran, int min, int max) {
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

		ItemMetadata itemMetadata = new ItemMetadata(id, version, device.getId(), parentId, parentVersion, status, modifiedAt, checksum,
				size, isFolder, filename, mimetype, chunks);
		itemMetadata.setChunks(chunks);
		itemMetadata.setTempId((long) ran.nextInt(10));

		return itemMetadata;
	}

}
