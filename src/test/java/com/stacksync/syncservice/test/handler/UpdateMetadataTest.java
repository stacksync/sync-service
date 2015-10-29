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

import java.util.UUID;

public class UpdateMetadataTest {

   private static APIHandler handler;
   private static InfinispanWorkspaceDAO workspaceDAO;
   private static InfinispanUserDAO userDao;
   private static UserRMI user1;
   private static UserRMI user2;

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
      WorkspaceRMI workspace1 = new WorkspaceRMI(UUID.randomUUID(), 1, user1.getId(), false, false);
      workspaceDAO.add(workspace1);

      user2 = new UserRMI(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
      userDao.add(user2);
      WorkspaceRMI workspace2 = new WorkspaceRMI(UUID.randomUUID(), 1, user2.getId(), false, false);
      workspaceDAO.add(workspace2);
   }

   @Test
   public void updateData() throws Exception {

      ItemMetadataRMI root = new ItemMetadataRMI();
      root.setFilename("/");
      root.setIsFolder(true);
      handler.createFolder(user1, root);

      ItemMetadataRMI file = new ItemMetadataRMI();
      file.setFilename("chunks-1.png");
      file.setParentId(root.getId());
      handler.createFile(user1, file);

      ItemMetadataRMI file2 = new ItemMetadataRMI();
      file2.setId(file.getId());
      file2.setFilename("chunks-2.png");
      APICommitResponse response = handler.updateMetadata(user1, file2);

      System.out.println(response.toString());
   }

}
