/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.test.handler;

import com.stacksync.commons.models.abe.ABEItemMetadata;
import com.stacksync.commons.models.abe.ABEMetaComponent;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.SyncMetadata;
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
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.handler.SyncHandler;
import com.stacksync.syncservice.rpc.parser.IParser;
import com.stacksync.syncservice.rpc.parser.JSONParser;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.test.dao.PostgresqlDAOTest;
import com.stacksync.syncservice.util.Config;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author javigd
 */
public class AbeHandlerTest {
    
	private static IParser reader;
        private static SyncHandler handler;
	private static ConnectionPool pool;
	private static Connection connection;
	private static WorkspaceDAO workspaceDAO;
	private static UserDAO userDao;
	private static DeviceDAO deviceDao;
	private static ItemDAO objectDao;
	private static ItemVersionDAO oversionDao;
        private static User user;
        private static Workspace workspace;
        private static Device device;
	private static UUID user1 = UUID.randomUUID();
    
        public AbeHandlerTest() {
        }

        @BeforeClass
        public static void prepareScenario() {
		try {
                        URL configFileResource = AbeHandlerTest.class.getResource("/log4j.xml");
                        System.out.println("Fetching URL: " + configFileResource.toString());
                        DOMConfigurator.configure(configFileResource);

                        Config.loadProperties();

                        String dataSource = "postgresql";
                        DAOFactory factory = new DAOFactory(dataSource);
                        connection = ConnectionPoolFactory.getConnectionPool(dataSource).getConnection();
                        
			pool = ConnectionPoolFactory.getConnectionPool(dataSource);
			reader = new JSONParser();

                        handler = new SQLSyncHandler(pool);
                                                
			workspaceDAO = factory.getWorkspaceDao(connection);
			userDao = factory.getUserDao(connection);
			deviceDao = factory.getDeviceDAO(connection);
			objectDao = factory.getItemDAO(connection);
			oversionDao = factory.getItemVersionDAO(connection);

			user = new User(user1, "abetester", "abetester", "AUTH_12312312", "a@a.a", 100, 0);
			userDao.add(user);

			workspace = new Workspace(null,  1, user, false, false);
                        workspace.setAbeEncrypted(true);
                        
			workspaceDAO.add(workspace);
                        workspace.setName("abetest");

                        workspaceDAO.addUser(user, workspace);
                        
			device = new Device(null, "junitdevice", user);
			deviceDao.add(device);

		} catch (Exception e) {
			e.printStackTrace();
		}
        }

        @AfterClass
        public static void clearScenario() {
                try {
                        userDao.delete(user.getId());
                } catch (DAOException e) {
                        e.printStackTrace();
                }
        }

        @Before
        public void setUp() {
        }

        @After
        public void tearDown() {
        }

        @Test
        public void abeCommitTest() {
            
                // Initialize test ABE meta components
                List<ABEMetaComponent> components = new ArrayList<ABEMetaComponent>(3);
                for (int i = 0; i < 3; i++) {
                    ABEMetaComponent component = new ABEMetaComponent();
                    component.setAttributeId("4659032a-1437-4bcb-a8eb-bb482e1e21a3");
                    component.setEncryptedPKComponent(("myencryptedPKComponent").getBytes());
                    component.setVersion(0L);
                    components.add(component);
                }
                
                String cipherSymKey = "myCipherSymKey";
                
                List<String> chunks = new ArrayList<String>();
                chunks.add("a"); 
                chunks.add("b");

                // Initialize the ABE Item Metadata object
                SyncMetadata item = new ABEItemMetadata(null, 1L, device.getId(), 
                        null, null, "NEW", Date.valueOf("2014-12-12"),
                        3499525671L, 1968L, false, "abetesting", "text/plain", chunks,
                        components, cipherSymKey.getBytes());
                
                List<SyncMetadata> metaList = new ArrayList<SyncMetadata>();
                metaList.add(item);
                
                try {
                    handler.doCommit(user, workspace, device, metaList);
                } catch (DAOException ex) {
                    Logger.getLogger(AbeHandlerTest.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
}
