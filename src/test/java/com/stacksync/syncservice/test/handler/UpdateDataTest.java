package com.stacksync.syncservice.test.handler;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.InfinispanUserDAO;
import com.stacksync.syncservice.db.infinispan.InfinispanWorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.handler.APIHandler;
import com.stacksync.syncservice.handler.SQLAPIHandler;
import com.stacksync.syncservice.rpc.messages.APICommitResponse;
import com.stacksync.syncservice.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

public class UpdateDataTest {

   private static APIHandler handler;
   private static InfinispanWorkspaceDAO workspaceDAO;
   private static InfinispanUserDAO userDao;
   private static UserRMI user1;
   private static UserRMI user2;
   private static WorkspaceRMI workspace1;

   @BeforeClass
   public static void initializeData() throws Exception {

      Config.loadProperties();

      String datasource = Config.getDatasource();
      ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

      handler = new SQLAPIHandler(pool);
      DAOFactory factory = new DAOFactory(datasource);

      Connection connection = pool.getConnection();


      workspaceDAO = factory.getWorkspaceDao(connection);
      userDao = factory.getUserDao(connection);

      user1 = new UserRMI(UUID.fromString("159a1286-33df-4453-bf80-cff4af0d97b0"), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

      userDao.add(user1);
      workspace1 = new WorkspaceRMI(UUID.randomUUID(), 1, user1.getId(), false, false);
      workspaceDAO.add(workspace1);

      user2 = new UserRMI(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);

      userDao.add(user2);
      WorkspaceRMI workspace2 = new WorkspaceRMI(UUID.randomUUID(), 1, user2.getId(), false, false);
      workspaceDAO.add(workspace2);
   }

   @Test
   public void updateData() throws Exception {

      ItemMetadataRMI file = new ItemMetadataRMI();
      file.setMimetype("image/jpeg");
      file.setChecksum(0000000000L);
      file.setSize(9999L);
      file.setChunks(Arrays.asList("11111", "22222", "333333"));
      file.setVersion(new Long(1));
      file.setIsFolder(false);
      file.setWorkspaceId(workspace1.getId());

      APICommitResponse response = handler.createFile(user1, file);
      response = handler.updateData(user1, response.getMetadata());
      System.out.println(response.toString());
   }

}
