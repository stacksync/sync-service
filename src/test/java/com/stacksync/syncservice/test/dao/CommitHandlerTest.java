/**
 * 
 */
package com.stacksync.syncservice.test.dao;

import static org.junit.Assert.*;

import java.io.IOException;
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

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.handler.CommitHandler;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.util.Config;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class CommitHandlerTest {

	private static final int CHUNK_SIZE = 512 * 1024;
	private static int NUM_TIMES = 1000;
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

	private static CommitHandler commitHandler;
	private static Handler handler;

	@BeforeClass
	public static void testSetup() throws ClassNotFoundException, SQLException, DAOException, IOException {
		Config.loadProperties("config.properties");
		
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

		try {
			commitHandler = new CommitHandler(connection);
			ConnectionPool pool = new PostgresqlConnectionPool(HOST, PORT, DB, USER, PASS, 10, 50);
			handler = new Handler(pool);
		} catch (NoStorageManagerAvailable e) {
			e.printStackTrace();
		} catch (DAOConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testTimes() throws DAOException {
		System.out.println("Commithandler test:");
		
		Random ran = new Random(System.currentTimeMillis());
		List<Long> commitHandlerTimes = new ArrayList<Long>();

		int i;
		long start, end, sum;
		for (i = 0; i < NUM_TIMES; i++) {
			// Create a ItemMetadata List
			List<ItemMetadata> items = new ArrayList<ItemMetadata>();
			items.add(createItemMetadata(ran, 1, 8));

			start = System.currentTimeMillis();
			commitHandler.doCommit(user, workspace, device, items);
			end = System.currentTimeMillis();

			commitHandlerTimes.add(end - start);
		}

		i = 0;
		sum = 0;
		for (Long t : commitHandlerTimes) {
			sum += t;
		}

		double mean = (double) (sum) / commitHandlerTimes.size();

		System.out.println("Mean: " + mean);

		double d = 0;
		for (Long t : commitHandlerTimes) {
			d += (t - mean) * (t - mean);
		}

		System.out.println("Dev: " + Math.sqrt(d / commitHandlerTimes.size()));

	}

	@Test
	public void testTimes2() throws DAOException {
		System.out.println("Handler test:");
		
		Random ran = new Random(System.currentTimeMillis());
		List<Long> commitHandlerTimes = new ArrayList<Long>();

		int i;
		long start, end, sum;
		for (i = 0; i < NUM_TIMES; i++) {
			// Create a ItemMetadata List
			List<ItemMetadata> items = new ArrayList<ItemMetadata>();
			items.add(createItemMetadata(ran, 1, 8));

			start = System.currentTimeMillis();
			handler.doCommit(user, workspace, device, items);
			end = System.currentTimeMillis();

			commitHandlerTimes.add(end - start);
		}

		i = 0;
		sum = 0;
		for (Long t : commitHandlerTimes) {
			sum += t;
		}

		double mean = (double) (sum) / commitHandlerTimes.size();

		System.out.println("Mean: " + mean);

		double d = 0;
		for (Long t : commitHandlerTimes) {
			d += (t - mean) * (t - mean);
		}

		System.out.println("Dev: " + Math.sqrt(d / commitHandlerTimes.size()));

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

	@AfterClass
	public static void testEnd() throws DAOException, SQLException {
		userDao.delete(user.getId()); // This should remove everything else...
		connection.close();
	}
}
