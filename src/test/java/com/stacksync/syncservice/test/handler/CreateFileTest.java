package com.stacksync.syncservice.test.handler;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.handler.APIHandler;
import com.stacksync.syncservice.handler.Handler.Status;
import com.stacksync.syncservice.handler.SQLAPIHandler;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.util.Config;
import com.stacksync.syncservice.util.Constants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class CreateFileTest {

   private static APIHandler handler;
   private static WorkspaceDAO workspaceDAO;
   private static UserDAO userDao;
   private static UserRMI user1;
   private static UserRMI user2;

   @BeforeClass
   public static void initializeData() throws Exception {

      Config.loadProperties();

      String datasource = Config.getDatasource();
      DAOFactory factory = new DAOFactory(datasource);
      ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
      Connection connection = pool.getConnection();
      connection.cleanup();

      handler = new SQLAPIHandler(pool);
      workspaceDAO = factory.getWorkspaceDao(connection);
      userDao = factory.getUserDao(connection);

      user1 = new UserRMI(UUID.fromString("159a1286-33df-4453-bf80-cff4af0d97b0"), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
      userDao.add(user1);

      WorkspaceRMI workspace1 = new WorkspaceRMI(UUID.randomUUID(), 1, user1.getId(), false, false);
      workspaceDAO.add(workspace1);

      user2 = new UserRMI(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
      userDao.add(user2);

      WorkspaceRMI workspace2 = new WorkspaceRMI(UUID.randomUUID(), 1, user2.getId(), false, false);
      workspaceDAO.add(workspace2);
   }

   @Test
   public void createNewFile() throws Exception {

      ItemMetadataRMI file = new ItemMetadataRMI();
      file.setFilename("holaaaa.txt");
      file.setParentId(null);
      file.setTempId(new Random().nextLong());
      file.setIsFolder(false);
      file.setVersion(1L);
      file.setDeviceId(Constants.API_DEVICE_ID);
      file.setMimetype("image/jpeg");
      file.setChecksum(0000000000L);
      file.setSize(9999L);
      file.setStatus(Status.NEW.toString());
      file.setModifiedAt(new Date());
      file.setChunks(Arrays.asList("11111", "22222", "333333"));
      file.setWorkspaceId(user1.getWorkspaces().iterator().next());

      APICommitResponse response = handler.createFile(user1, file);
      System.out.println(response.toString());
   }

}
