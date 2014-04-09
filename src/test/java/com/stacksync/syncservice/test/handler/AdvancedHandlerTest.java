package com.stacksync.syncservice.test.handler;

import java.sql.Connection;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.DeviceDAO;
import com.stacksync.syncservice.db.ItemDAO;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.UserDAO;
import com.stacksync.syncservice.db.WorkspaceDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.rpc.parser.IParser;
import com.stacksync.syncservice.rpc.parser.JSONParser;
import com.stacksync.syncservice.util.Config;

public class AdvancedHandlerTest {

	// TODO quitar throws

	private static IParser reader;
	private static ConnectionPool pool;
	private static Connection connection;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static DeviceDAO deviceDao;
	private static ItemDAO objectDao;
	private static ItemVersionDAO oversionDao;
	private static UUID user1 = UUID.randomUUID();
	
	
	@BeforeClass
	public static void initializeData() {

		try {
			Config.loadProperties();
			String datasource = Config.getDatasource();

			pool = ConnectionPoolFactory.getConnectionPool(datasource);
			reader = new JSONParser();

			DAOFactory factory = new DAOFactory(datasource);

			workspaceDAO = factory.getWorkspaceDao(connection);
			userDao = factory.getUserDao(connection);
			deviceDao = factory.getDeviceDAO(connection);
			objectDao = factory.getItemDAO(connection);
			oversionDao = factory.getItemVersionDAO(connection);


			User user = new User(user1, "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
			userDao.add(user);

			Workspace workspace = new Workspace(null,  1, user, false, false);
			workspaceDAO.add(workspace);

			Device device = new Device(null, "junitdevice", user);
			deviceDao.add(device);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void cleanData() throws DAOException {
		userDao.delete(user1);
	}

	@Before
	public void fillDB() throws DAOException {

		String data = "{" + "'user':'junituser'," + "'type':'commit'," + "'workspace':'junituser/'," + "'requestId':'junitdevice-1361792472697',"
				+ "'device':'junitdevice'," + "'metadata':[{" + "'rootId':'stacksync'," + "'fileId':1," + "'version':1," + "'parentRootId':'',"
				+ "'parentFileId':''," + "'parentFileVersion':''," + "'updated':1362495000105," + "'status':'NEW'," + "'lastModified':1362495000105,"
				+ "'checksum':3499525671," + "'clientName':'junitdevice'," + "'fileSize':1968," + "'folder':'0'," + "'name':'pgadmin.log'," + "'path':'/',"
				+ "'mimetype':'text/plain'," + "'chunks':['a']}," + "{" + "'rootId':'stacksync'," + "'fileId':1," + "'version':2," + "'parentRootId':'',"
				+ "'parentFileId':''," + "'parentFileVersion':''," + "'updated':1362495999105," + "'status':'CHANGED'," + "'lastModified':1362495999105,"
				+ "'checksum':3499525600," + "'clientName':'junitdevice'," + "'fileSize':900," + "'folder':'0'," + "'name':'pgadmin.log'," + "'path':'/',"
				+ "'mimetype':'text/plain'," + "'chunks':['a','b']}," + "{" + "'rootId':'stacksync'," + "'fileId':1," + "'version':3," + "'parentRootId':'',"
				+ "'parentFileId':''," + "'parentFileVersion':''," + "'updated':1362496999105," + "'status':'CHANGED'," + "'lastModified':1362496999105,"
				+ "'checksum':3499525600," + "'clientName':'junitdevice'," + "'fileSize':1900," + "'folder':'0'," + "'name':'pgadmin.log'," + "'path':'/',"
				+ "'mimetype':'text/plain'," + "'chunks':['b','c']}]}";

		byte[] firstCommitByte = data.getBytes();
		/*
		 * Commit commitFirst = (Commit) reader.readMessage(firstCommitByte);
		 * handler.doCommit(commitFirst);
		 */
	}

	@After
	public void cleanDB() throws DAOException {
		// TODO:
		// objectDao.deleteByFileIdAndWorkspace(1, "junituser/");
	}

	@Test
	public void conflictWithChunks() throws DAOException {

		String conflictVersion = "{" + "'user':'junituser'," + "'type':'commit'," + "'workspace':'junituser/'," + "'requestId':'junitdevice-1361792472697',"
				+ "'device':'junitdevice'," + "'metadata':[{" + "'rootId':'stacksync'," + "'fileId':1," + "'version':3," + "'parentRootId':'',"
				+ "'parentFileId':''," + "'parentFileVersion':''," + "'updated':1362497999105," + "'status':'CHANGED'," + "'lastModified':1362497999105,"
				+ "'checksum':3499525600," + "'clientName':'junitdevice'," + "'fileSize':2000," + "'folder':'0'," + "'name':'pgadmin.log'," + "'path':'/',"
				+ "'mimetype':'text/plain'," + "'chunks':['c','e']}]}";

		byte[] conflictVersionByte = conflictVersion.getBytes();
		/*
		 * Commit commitConflictVersion = (Commit)
		 * reader.readMessage(conflictVersionByte); CommitResponse crm =
		 * handler.doCommit(commitConflictVersion);
		 * 
		 * List<RemoteCommitResponseObject> objects = crm.getObjects();
		 * RemoteCommitResponseObject object = (RemoteCommitResponseObject)
		 * objects.get(0);
		 * 
		 * // Check if all commits are true. assertFalse("Object committed.",
		 * object.isCommitted()); // Check if 3 is the server version
		 * assertTrue("Correct version.", object.getVersion() == 3);
		 * 
		 * ObjectMetadata correctMetadata = object.getMetadata(); ObjectMetadata
		 * conflictMetadata = commitConflictVersion.getObjects().get(0);
		 * 
		 * assertFalse("Incorrect metadata.",
		 * conflictMetadata.equals(correctMetadata));
		 * 
		 * List<String> conflictChunks = conflictMetadata.getChunks();
		 * assertTrue("Same chunks", conflictChunks.get(0).equals("c"));
		 * assertTrue("Same chunks", conflictChunks.get(1).equals("e"));
		 * 
		 * List<String> correctChunks = correctMetadata.getChunks();
		 * assertTrue("Same chunks", correctChunks.get(0).equals("b"));
		 * assertTrue("Same chunks", correctChunks.get(1).equals("c"));
		 */
	}

	@Test
	public void conflictWithChecksum() throws DAOException {

		String conflictVersion = "{" + "'user':'junituser'," + "'type':'commit'," + "'workspace':'junituser/'," + "'requestId':'junitdevice-1361792472697',"
				+ "'device':'junitdevice'," + "'metadata':[{" + "'rootId':'stacksync'," + "'fileId':1," + "'version':3," + "'parentRootId':'',"
				+ "'parentFileId':''," + "'parentFileVersion':''," + "'updated':1362497999105," + "'status':'RENAMED'," + "'lastModified':1362497999105,"
				+ "'checksum':3499525600," + "'clientName':'junitdevice'," + "'fileSize':2000," + "'folder':'0'," + "'name':'conflict.log'," + "'path':'/',"
				+ "'mimetype':'text/plain'," + "'chunks':['b','c']}]}";

		/*
		 * byte[] conflictVersionByte = conflictVersion.getBytes(); Commit
		 * commitConflictVersion = (Commit)
		 * reader.readMessage(conflictVersionByte); CommitResponse crm =
		 * handler.doCommit(commitConflictVersion);
		 * 
		 * List<RemoteCommitResponseObject> objects = crm.getObjects();
		 * RemoteCommitResponseObject object = (RemoteCommitResponseObject)
		 * objects.get(0);
		 * 
		 * // Check if all commits are true. assertFalse("Object committed.",
		 * object.isCommitted()); // Check if 3 is the server version
		 * assertTrue("Correct version.", object.getVersion() == 3);
		 * 
		 * ObjectMetadata correctMetadata = object.getMetadata(); ObjectMetadata
		 * conflictMetadata = commitConflictVersion.getObjects().get(0);
		 * 
		 * assertFalse("Incorrect metadata.",
		 * conflictMetadata.equals(correctMetadata));
		 * 
		 * assertFalse("Incorrect name.",
		 * conflictMetadata.getFileName().equals(correctMetadata
		 * .getFileName())); assertFalse("Incorrect status.",
		 * conflictMetadata.getStatus().equals(correctMetadata.getStatus()));
		 */
	}

	@Test
	public void conflictWithName() throws DAOException {
		String conflictVersion = "{" + "'user':'junituser'," + "'type':'commit'," + "'workspace':'junituser/'," + "'requestId':'junitdevice-1361792472697',"
				+ "'device':'junitdevice'," + "'metadata':[{" + "'rootId':'stacksync'," + "'fileId':1," + "'version':3," + "'parentRootId':'',"
				+ "'parentFileId':''," + "'parentFileVersion':''," + "'updated':1362497999105," + "'status':'CHANGED'," + "'lastModified':1362497999105,"
				+ "'checksum':999," + "'clientName':'junitdevice'," + "'fileSize':2000," + "'folder':'0'," + "'name':'pgadmin.log'," + "'path':'/',"
				+ "'mimetype':'text/plain'," + "'chunks':['b','c']}]}";

		/*
		 * byte[] conflictVersionByte = conflictVersion.getBytes(); Commit
		 * commitConflictVersion = (Commit)
		 * reader.readMessage(conflictVersionByte); CommitResponse crm =
		 * handler.doCommit(commitConflictVersion);
		 * 
		 * List<RemoteCommitResponseObject> objects = crm.getObjects();
		 * RemoteCommitResponseObject object = (RemoteCommitResponseObject)
		 * objects.get(0);
		 * 
		 * // Check if all commits are true. assertFalse("Object committed.",
		 * object.isCommitted()); // Check if 3 is the server version
		 * assertTrue("Correct version.", object.getVersion() == 3);
		 * 
		 * ObjectMetadata correctMetadata = object.getMetadata(); ObjectMetadata
		 * conflictMetadata = commitConflictVersion.getObjects().get(0);
		 * 
		 * assertFalse("Incorrect metadata.",
		 * conflictMetadata.equals(correctMetadata));
		 * 
		 * assertFalse("Incorrect checksum.", conflictMetadata.getChecksum() ==
		 * correctMetadata.getChecksum());
		 */
	}

}
