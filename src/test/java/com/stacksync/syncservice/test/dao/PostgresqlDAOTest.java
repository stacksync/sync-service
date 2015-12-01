package com.stacksync.syncservice.test.dao;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.ItemDAO;
import com.stacksync.syncservice.db.infinispan.ItemVersionDAO;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.util.Config;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PostgresqlDAOTest {

   private static Connection connection;
   private static WorkspaceDAO workspaceDAO;
   private static UserDAO userDao;
   private static ItemDAO objectDao;
   private static ItemVersionDAO oversionDao;
   private static SecureRandom random = new SecureRandom();

   @BeforeClass
   public static void testSetup() throws IOException, SQLException, DAOConfigurationException, Exception {

      URL configFileResource = PostgresqlDAOTest.class.getResource("/com/ast/processserver/resources/log4j.xml");
      DOMConfigurator.configure(configFileResource);

      Config.loadProperties();

      String dataSource = "postgresql";
      DAOFactory factory = new DAOFactory(dataSource);
      connection = ConnectionPoolFactory.getConnectionPool(dataSource).getConnection();
      workspaceDAO = factory.getDAO(connection);
      userDao = factory.getDAO(connection);
      objectDao = factory.getDAO(connection);
      oversionDao = factory.getDAO(connection);
   }

   protected String nextString() {
      return new BigInteger(130, random).toString(32);
   }

   @Test
   public void testCreateNewValidUser() throws IllegalArgumentException, DAOException, RemoteException {
      UserRMI user = new UserRMI();
      user.setName(nextString());
      user.setId(UUID.randomUUID());
      user.setEmail(nextString());
      user.setQuotaLimit(2048);
      user.setQuotaUsed(1403);

      userDao.add(user);

      if (user.getId() == null) {
         assertTrue("Could not retrieve the User ID", false);
      } else {
         assertTrue(true);
      }

   }

   @Test
   public void testCreateNewUserSameId() throws IllegalArgumentException, DAOException, RemoteException {

      UUID userId = UUID.randomUUID();

      UserRMI user = new UserRMI();
      user.setName(nextString());
      user.setId(userId);
      user.setEmail(nextString());
      user.setQuotaLimit(2048);
      user.setQuotaUsed(1403);

      userDao.add(user);

      if (user.getId() == null) {
         assertTrue("Could not retrieve the User ID", false);
      } else {
         UserRMI user2 = new UserRMI();
         user2.setName(nextString());
         user2.setId(userId);
         user2.setEmail(nextString());
         user2.setQuotaLimit(2048);
         user2.setQuotaUsed(1403);

         userDao.add(user2);
         assertTrue("User should not have been created", false);
      }
   }

   @Test
   public void testUpdateExistingUserOk() throws IllegalArgumentException, DAOException, RemoteException {

      UserRMI user = new UserRMI();
      user.setName(nextString());
      user.setId(UUID.randomUUID());
      user.setEmail(nextString());
      user.setQuotaLimit(2048);
      user.setQuotaUsed(1403);

      userDao.add(user);

      if (user.getId() == null) {
         assertTrue("Could not retrieve the User ID", false);
      } else {

         UUID id = user.getId();
         String newName = nextString();
         UUID newUserId = UUID.randomUUID();
         String newEmail = nextString();
         Integer newQuotaLimit = 123;
         Integer newQuotaUsed = 321;

         user.setName(newName);
         user.setId(newUserId);
         user.setEmail(newEmail);
         user.setQuotaLimit(newQuotaLimit);
         user.setQuotaUsed(newQuotaUsed);

         userDao.add(user);
         UserRMI user2 = userDao.findById(id);
         assertEquals(user, user2);
      }
   }

   @Test
   public void testGetNonExistingUserById() throws RemoteException {
      UserRMI user = userDao.findById(UUID.randomUUID());
      if (user == null) {
         assertTrue(true);
      } else {
         assertTrue("User should not exist", false);
      }
   }

   @Test
   public void testGetExistingUserById() throws IllegalArgumentException, DAOException, RemoteException {

      UserRMI user = new UserRMI();
      user.setName(nextString());
      user.setId(UUID.randomUUID());
      user.setEmail(nextString());
      user.setQuotaLimit(2048);
      user.setQuotaUsed(1403);

      userDao.add(user);

      if (user.getId() == null) {
         assertTrue("Could not retrieve the User ID", false);
      } else {

         UserRMI user2 = userDao.findById(user.getId());
         if (user2 == null) {
            assertTrue("User should exist", false);
         } else {
            if (user2.getId() != null && user2.isValid()) {
               assertTrue(true);
            } else {
               assertTrue("User is not valid", false);
            }
         }
      }
   }

   @Test
   public void testCreateNewWorkspaceInvalidOwner() throws RemoteException {

      UserRMI user = new UserRMI(UUID.randomUUID());

      WorkspaceRMI workspace = new WorkspaceRMI(UUID.randomUUID());
      workspace.setOwner(user);

      workspaceDAO.add(workspace);
      assertTrue("User should not have been created", false);
   }

   @Test
   public void testCreateNewWorkspaceValidOwner() throws IllegalArgumentException, DAOException, RemoteException {

      UserRMI user = new UserRMI();
      user.setName(nextString());
      user.setId(UUID.randomUUID());
      user.setEmail(nextString());
      user.setQuotaLimit(2048);
      user.setQuotaUsed(1403);
      userDao.add(user);

      WorkspaceRMI workspace = new WorkspaceRMI(UUID.randomUUID());
      workspace.setLatestRevision(0);
      workspace.setOwner(user);

      workspaceDAO.add(workspace);
      assertTrue(true);
   }

   @Test
   public void testCreateObjectInvalidWorkspace() throws IllegalArgumentException, DAOException, RemoteException {

      UserRMI user = new UserRMI();
      user.setName(nextString());
      user.setId(UUID.randomUUID());
      user.setEmail(nextString());
      user.setQuotaLimit(2048);
      user.setQuotaUsed(1403);
      userDao.add(user);

      WorkspaceRMI workspace = new WorkspaceRMI(UUID.randomUUID());
      workspace.setOwner(user);
      workspace.setLatestRevision(0);

      ItemRMI object = new ItemRMI(
            random.nextLong(),
            workspace.getId(),
            1L,
            null,
            nextString(),
            "image/jpeg",
            false,
            1L);

      objectDao.add(object);
      assertTrue("Object should not have been created", false);

   }


   @Test
   public void testGetObjectByClientFileIdAndWorkspace() throws DAOException, RemoteException {

      long fileId = 4852407995043916970L;
      objectDao.findById(fileId);

      // TODO Check if the returned obj is correct
   }

   @Test
   public void testGetWorkspaceById() {

   }

   @Test
   public void testGetObjectMetadataByWorkspaceName() throws DAOException, RemoteException {

      List<ItemMetadataRMI> objects = objectDao.getItemsByWorkspaceId(UUID.randomUUID());

      if (objects != null && !objects.isEmpty()) {

         for (ItemMetadataRMI object : objects) {
            System.out.println(object.toString());
         }

         assertTrue(true);
      } else {
         assertTrue(false);
      }
   }

   @Test
   public void testGetObjectMetadataByClientFileIdWithoutChunks() throws DAOException, RemoteException {

      Long fileId = 538757639L;
      boolean includeDeleted = false;
      boolean includeChunks = false;
      Long version = 1L;
      boolean list = true;

      ItemMetadataRMI object = objectDao.findById(fileId, list, version, includeDeleted, includeChunks);

      if (object != null) {
         System.out.println(object.toString());

         if (object.getChildren() != null) {
            for (ItemMetadataRMI child : object.getChildren()) {
               System.out.println(child.toString());
            }
         }
         assertTrue(true);
      } else {
         assertTrue(false);
      }
   }

   @Test
   public void testGetObjectMetadataByClientFileIdWithChunks() throws DAOException, RemoteException {

      Long fileId = 538757639L;
      boolean includeDeleted = false;
      boolean includeChunks = true;
      Long version = 1L;
      boolean list = true;

      ItemMetadataRMI object = objectDao.findById(fileId, list, version, includeDeleted, includeChunks);

      if (object != null) {
         System.out.println(object.toString());

         if (object.getChildren() != null) {
            for (ItemMetadataRMI child : object.getChildren()) {
               System.out.println(child.toString());
            }
         }
         assertTrue(true);
      } else {
         assertTrue(false);
      }
   }

   @Test
   public void testGetObjectMetadataByServerUserId() throws DAOException, RemoteException {

      UUID userId = UUID.randomUUID();
      boolean includeDeleted = false;

      ItemMetadataRMI object = objectDao.findByUserId(userId, includeDeleted);

      if (object != null) {
         System.out.println(object.toString());

         if (object.getChildren() != null) {
            for (ItemMetadataRMI child : object.getChildren()) {
               System.out.println(child.toString());
            }
         }
         assertTrue(true);
      } else {
         assertTrue(false);
      }
   }

}
