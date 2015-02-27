package com.stacksync.syncservice.test.handler;

import com.stacksync.syncservice.db.Connection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanUserDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.rpc.messages.APIGetMetadata;
import com.stacksync.syncservice.rpc.parser.IParser;
import com.stacksync.syncservice.rpc.parser.JSONParser;
import com.stacksync.syncservice.util.Config;

public class GetMetadataTest {

	private static IParser reader;
	private static InfinispanUserDAO userDao;

	@BeforeClass
	public static void initializeData() {

		try {
			Config.loadProperties();
			reader = new JSONParser();

			String datasource = Config.getDatasource();
			ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

			DAOFactory factory = new DAOFactory(datasource);

			Connection connection = pool.getConnection();

			userDao = factory.getUserDao(connection);

			/*
			 * UNCOMMENT TO INITIALIZE DATABASE (SLOW)
			 * 
			 * DBBenchmark benchmark; int levels = 2; try { benchmark = new
			 * DBBenchmark(levels); benchmark.fillDB(); } catch (Exception e) {
			 * e.printStackTrace(); }
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void cleanData() throws DAOException {
		//userDao.delete("bb");
	}

	@Test
	public void getMetadataByCorrectFileId() throws DAOException {

		/*
		 * String query = "{ " + "'user': 'bb', " + "'type': 'get_metadata'," +
		 * "'fileId': '538757639', " + "'list': 'true', " +
		 * "'include_deleted': 'false', " + "'version': '1' " + "}";
		 * 
		 * byte[] queryByte = query.getBytes();
		 * 
		 * GetMetadataMessage msg = (GetMetadataMessage)
		 * reader.readMessage(queryByte); GetMetadataResponseMessage response =
		 * handler.doGetMetadata(msg);
		 * 
		 * assertTrue(response.getSucced());
		 * 
		 * printResponse(response);
		 */
	}

	@Test
	public void getMetadataByCorrectFileIdWithChunks() throws DAOException {

		/*
		 * String query = "{ " + "'user': 'bb', " + "'type': 'get_metadata'," +
		 * "'fileId': '538757639', " + "'list': 'true', " +
		 * "'include_deleted': 'false', " + "'include_chunks': 'true' " + "}";
		 * 
		 * byte[] queryByte = query.getBytes();
		 * 
		 * GetMetadataMessage msg = (GetMetadataMessage)
		 * reader.readMessage(queryByte); GetMetadataResponseMessage response =
		 * handler.doGetMetadata(msg);
		 * 
		 * assertTrue(response.getSucced());
		 * 
		 * printResponse(response);
		 */
	}

	@Test
	public void getMetadataByIncorrectFileId() throws DAOException {

		/*
		 * String query = "{ " + "'user': 'bb', " + "'type': 'get_metadata'," +
		 * "'fileId': '111111', " + "'list': 'true', " +
		 * "'include_deleted': 'false', " + "'version': '1' " + "}";
		 * 
		 * byte[] queryByte = query.getBytes();
		 * 
		 * GetMetadataMessage msg = (GetMetadataMessage)
		 * reader.readMessage(queryByte); GetMetadataResponseMessage response =
		 * handler.doGetMetadata(msg);
		 * 
		 * assertTrue(!response.getSucced() && response.getErrorCode() == 404);
		 * 
		 * printResponse(response);
		 */
	}

	@Test
	public void getMetadataByCorrectFileIdIncorrectVersion() throws DAOException {

		/*
		 * String query = "{ " + "'user': 'bb', " + "'type': 'get_metadata'," +
		 * "'fileId': '538757639', " + "'list': 'true', " +
		 * "'include_deleted': 'false', " + "'version': '1111' " + "}";
		 * 
		 * byte[] queryByte = query.getBytes();
		 * 
		 * GetMetadataMessage msg = (GetMetadataMessage)
		 * reader.readMessage(queryByte); GetMetadataResponseMessage response =
		 * handler.doGetMetadata(msg);
		 * 
		 * assertTrue(!response.getSucced() && response.getErrorCode() == 404);
		 * 
		 * printResponse(response);
		 */
	}

	@Test
	public void getMetadataByFileIdUserNotAuthorized() throws DAOException {

		/*
		 * String query = "{ " + "'user': 'asd', " + "'type': 'get_metadata'," +
		 * "'fileId': '538757639', " + "'list': 'true', " +
		 * "'include_deleted': 'false', " + "'version': '1' " + "}";
		 * 
		 * byte[] queryByte = query.getBytes();
		 * 
		 * GetMetadataMessage msg = (GetMetadataMessage)
		 * reader.readMessage(queryByte); GetMetadataResponseMessage response =
		 * handler.doGetMetadata(msg);
		 * 
		 * assertTrue(!response.getSucced() && response.getErrorCode() ==
		 * DAOError.USER_NOT_AUTHORIZED.getCode());
		 * 
		 * printResponse(response);
		 */
	}

	@Test
	public void getMetadataByServerUserIdCorrect() throws DAOException {

		/*
		 * String query = "{ " + "'user': 'bb', " + "'type': 'get_metadata'," +
		 * "'include_deleted': 'false' " + "}";
		 * 
		 * byte[] queryByte = query.getBytes();
		 * 
		 * GetMetadataMessage msg = (GetMetadataMessage)
		 * reader.readMessage(queryByte); GetMetadataResponseMessage response =
		 * handler.doGetMetadata(msg);
		 * 
		 * assertTrue(response.getSucced());
		 * 
		 * printResponse(response);
		 */
	}

	private void printResponse(APIGetMetadata r) {
		String response = reader.createResponse(r);
		System.out.println(response);
	}
}
