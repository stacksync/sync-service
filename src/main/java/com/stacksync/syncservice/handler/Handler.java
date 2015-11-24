package com.stacksync.syncservice.handler;

import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.*;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.exceptions.CommitExistantVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersion;
import com.stacksync.syncservice.exceptions.CommitWrongVersionNoParent;
import com.stacksync.syncservice.exceptions.InternalServerError;
import com.stacksync.syncservice.storage.StorageFactory;
import com.stacksync.syncservice.storage.StorageManager;
import com.stacksync.syncservice.storage.StorageManager.StorageType;
import com.stacksync.syncservice.util.Config;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;

public class Handler {

   private static final Logger logger = Logger.getLogger(Handler.class
         .getName());
   protected Connection connection;
   protected WorkspaceDAO workspaceDAO;
   protected UserDAO userDao;
   protected DeviceDAO deviceDao;
   protected ItemDAO itemDao;
   protected ItemVersionDAO itemVersionDao;
   protected StorageManager storageManager;
   protected static Random random = new Random(System.currentTimeMillis());

   public enum Status {
      NEW, DELETED, CHANGED, RENAMED, MOVED
   };

   public Handler(ConnectionPool pool) throws Exception {
      connection = pool.getConnection();

      String dataSource = Config.getDatasource();
      DAOFactory factory = new DAOFactory(dataSource);

      workspaceDAO = factory.getWorkspaceDao(connection);
      deviceDao = factory.getDeviceDAO(connection);
      userDao = factory.getUserDao(connection);
      itemDao = factory.getItemDAO(connection);
      itemVersionDao = factory.getItemVersionDAO(connection);
      storageManager = StorageFactory.getStorageManager(StorageType.SWIFT);
   }
   
   public void createUser(UUID id) throws Exception {
       UserRMI user = new UserRMI(id, id.toString(), id.toString(), null, "a@a.a", 0, 0);
       userDao.add(user);
       WorkspaceRMI workspace = new WorkspaceRMI(id, 0, id, false, false);
       workspaceDAO.add(workspace);
   }

   public WorkspaceRMI getWorkspace(UUID id) throws RemoteException {
      return workspaceDAO.getById(id);
   }

   public List<CommitInfo> doCommit(UserRMI user, WorkspaceRMI workspace,
         DeviceRMI device, List<ItemMetadataRMI> items) throws Exception {

      HashMap<Long, Long> tempIds = new HashMap<Long, Long>();

//      try {
//         workspace = workspaceDAO.getById(workspace.getId());
//         user = userDao.findById(user.getId());
//         // TODO: check if the workspace belongs to the user or its been given
//         // access
//
//         device = deviceDao.get(device.getId());
//
//      } catch (RemoteException ex) {
//         logger.error("Remote Exception getting workspace or device: " + ex);
//         throw new DAOException(ex);
//      }
//
//      // TODO: check if the device belongs to the user

      List<CommitInfo> responseObjects = new ArrayList<CommitInfo>();

      for (ItemMetadataRMI itemMetadata : items) {

         ItemMetadataRMI objectResponse = null;
         boolean committed;

         if (itemMetadata.getParentId() != null) {
            Long parentId = tempIds.get(itemMetadata.getParentId());
            if (parentId != null) {
               itemMetadata.setParentId(parentId);
            }
         }

         // if the itemMetadata does not have ID but has a TempID, maybe it was
         // set
         if (itemMetadata.getId() == null && itemMetadata.getTempId() != null) {
            Long newId = tempIds.get(itemMetadata.getTempId());
            if (newId != null) {
               itemMetadata.setId(newId);
            }
         }

         this.commitObject(itemMetadata, workspace, device);

         if (itemMetadata.getTempId() != null) {
            tempIds.put(itemMetadata.getTempId(), itemMetadata.getId());
         }

         objectResponse = itemMetadata;
         committed = true;

         responseObjects.add(new CommitInfo(itemMetadata.getVersion(), committed,
               objectResponse.toMetadataItem()));
      }

      return responseObjects;
   }

   public WorkspaceRMI doShareFolder(UserRMI user, List<String> emails, ItemRMI item,
         boolean isEncrypted) throws ShareProposalNotCreatedException,
         UserNotFoundException {

        /*// Check the owner
         try {
         user = userDao.findById(user.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Get folder metadata
         try {
         item = itemDao.findById(item.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         if (item == null || !item.isFolder()) {
         throw new ShareProposalNotCreatedException(
         "No folder found with the given ID.");
         }

         // Get the source workspace
         WorkspaceRMI sourceWorkspace;
         try {
         sourceWorkspace = workspaceDAO.getById(item.getWorkspace().getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
         if (sourceWorkspace == null) {
         throw new ShareProposalNotCreatedException("Workspace not found.");
         }

         // Check the addressees
         List<UserRMI> addressees = new ArrayList<UserRMI>();
         for (String email : emails) {
         UserRMI addressee;
         try {
         addressee = userDao.getByEmail(email);
         if (!addressee.getId().equals(user.getId())) {
         addressees.add(addressee);
         }

         } catch (IllegalArgumentException e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         } catch (Exception e) {
         logger.warn(
         String.format(
         "Email '%s' does not correspond with any user. ",
         email), e);
         }
         }

         if (addressees.isEmpty()) {
         throw new ShareProposalNotCreatedException("No addressees found");
         }

         WorkspaceRMI workspace;

         if (sourceWorkspace.isShared()) {
         workspace = sourceWorkspace;

         } else {
         // Create the new workspace
         String container = UUID.randomUUID().toString();

         workspace = new WorkspaceRMI();
         workspace.setShared(true);
         workspace.setEncrypted(isEncrypted);
         workspace.setName(item.getFilename());
         workspace.setOwner(user.getId());
         List<UUID> users = new ArrayList<UUID>();
         for (UserRMI userInList : addressees) {
         users.add(userInList.getId());
         }
         workspace.setUsers(users);
         workspace.setSwiftContainer(container);
         workspace.setSwiftUrl(Config.getSwiftUrl() + "/"
         + user.getSwiftAccount());

         // Create container in Swift
         try {
         storageManager.createNewWorkspace(workspace);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Save the workspace to the DB
         try {
         workspaceDAO.add(workspace);
         // add the owner to the workspace
         workspaceDAO.addUser(user, workspace);

         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Grant user to container in Swift
         try {
         storageManager.grantUserToWorkspace(user, user, workspace);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Migrate files to new workspace
         List<String> chunks;
         try {
         chunks = itemDao.migrateItem(item.getId(), workspace.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Move chunks to new container
         for (String chunkName : chunks) {
         try {
         storageManager.copyChunk(sourceWorkspace, workspace,
         chunkName);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
         }
         }

         // Add the addressees to the workspace
         for (UserRMI addressee : addressees) {
         try {
         workspaceDAO.addUser(addressee, workspace);

         } catch (Exception e) {
         workspace.getUsers().remove(addressee);
         logger.error(
         String.format(
         "An error ocurred when adding the user '%s' to workspace '%s'",
         addressee.getId(), workspace.getId()), e);
         }

         // Grant the user to container in Swift
         try {
         storageManager.grantUserToWorkspace(user, addressee, workspace);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
         }

         return workspace;*/
      return null;
   }

   public UnshareData doUnshareFolder(UserRMI user, List<String> emails, ItemRMI item, boolean isEncrypted)
         throws ShareProposalNotCreatedException, UserNotFoundException {

        /*UnshareData response;
         // Check the owner
         try {
         user = userDao.findById(user.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Get folder metadata
         try {
         item = itemDao.findById(item.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         if (item == null || !item.isFolder()) {
         throw new ShareProposalNotCreatedException("No folder found with the given ID.");
         }

         // Get the workspace
         WorkspaceRMI sourceWorkspace;
         try {
         sourceWorkspace = workspaceDAO.getById(item.getWorkspace().getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
         if (sourceWorkspace == null) {
         throw new ShareProposalNotCreatedException("Workspace not found.");
         }
         if (!sourceWorkspace.isShared()) {
         throw new ShareProposalNotCreatedException("This workspace is not shared.");
         }
		
         // Check the addressees
         List<UserRMI> addressees = new ArrayList<UserRMI>();
         for (String email : emails) {
         UserRMI addressee;
         try {
         addressee = userDao.getByEmail(email);
         if (addressee.getId().equals(sourceWorkspace.getOwner())){
         logger.warn(String.format("Email '%s' corresponds with owner of the folder. ", email));
         throw new ShareProposalNotCreatedException("Email "+email+" corresponds with owner of the folder.");
				
         }
				
         if (!addressee.getId().equals(user.getId())) {
         addressees.add(addressee);
         }
				

         } catch (IllegalArgumentException e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         } catch (Exception e) {
         logger.warn(String.format("Email '%s' does not correspond with any user. ", email), e);
         }
         }

         if (addressees.isEmpty()) {
         throw new ShareProposalNotCreatedException("No addressees found");
         }

         // get workspace members
         List<UserWorkspaceRMI> workspaceMembers;
         try {
         workspaceMembers = doGetWorkspaceMembers(user, sourceWorkspace);
         } catch (InternalServerError e1) {
         throw new ShareProposalNotCreatedException(e1.toString());
         }

         // remove users from workspace
         List<UserRMI> usersToRemove = new ArrayList<UserRMI>();
		
         for (UserRMI userToRemove : addressees) {
         for (UserWorkspaceRMI member : workspaceMembers) {
         if (member.getUser().getEmail().equals(userToRemove.getEmail())) {
         workspaceMembers.remove(member);
         usersToRemove.add(userToRemove);
         break;
         }
         }
         }

         if (workspaceMembers.size() <= 1) {
         // All members have been removed from the workspace
         WorkspaceRMI defaultWorkspace;
         try {
         //Always the last member of a shared folder should be the owner
         defaultWorkspace = workspaceDAO.getDefaultWorkspaceByUserId(sourceWorkspace.getOwner());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException("Could not get default workspace");
         }

         // Migrate files to new workspace
         List<String> chunks;
         try {
         chunks = itemDao.migrateItem(item.getId(), defaultWorkspace.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }

         // Move chunks to new container
         for (String chunkName : chunks) {
         try {
         storageManager.copyChunk(sourceWorkspace, defaultWorkspace, chunkName);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
         }
			
         // delete workspace
         try {
         workspaceDAO.deleteWorkspace(sourceWorkspace.getId());
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
			
         // delete container from swift
         try {
         storageManager.deleteWorkspace(sourceWorkspace);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
			
         response = new UnshareData(usersToRemove, sourceWorkspace, true);

         } else {
			
         for(UserRMI userToRemove : usersToRemove){
				
         try {
         workspaceDAO.deleteUser(userToRemove, sourceWorkspace);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
				
         try {
         storageManager.removeUserToWorkspace(user, userToRemove, sourceWorkspace);
         } catch (Exception e) {
         logger.error(e);
         throw new ShareProposalNotCreatedException(e);
         }
         }
         response = new UnshareData(usersToRemove, sourceWorkspace, false);

         }
         return response;*/
      return null;
   }

   public List<UserRMI> doGetWorkspaceMembers(UserRMI user,
         WorkspaceRMI workspace) throws InternalServerError {

      // TODO: check user permissions.

      List<UserRMI> members;
      try {
         members = workspaceDAO.getMembersById(workspace.getId());
      } catch (Exception e) {
         logger.error(e);
         throw new InternalServerError(e);
      }

      if (members == null || members.isEmpty()) {
         throw new InternalServerError("No members found in workspace.");
      }

      return members;
   }

   public Connection getConnection() {
      return this.connection;
   }

   /*
    * Private functions
    */
   private void commitObject(ItemMetadataRMI itemMetadata, WorkspaceRMI workspace,
         DeviceRMI device) throws CommitWrongVersionNoParent,
         CommitWrongVersion, CommitExistantVersion, Exception {

      ItemRMI serverItem = itemDao.findById(itemMetadata.getId());

      // Check if this object already exists in the server.
      if (serverItem == null) {
         if (itemMetadata.getVersion() == 1) {
            this.saveNewObject(itemMetadata, workspace, device);
         } else {
            throw new CommitWrongVersionNoParent();
         }
         return;
      }

      // Check if the client version already exists in the server
      long serverVersion = serverItem.getLatestVersionNumber();
      long clientVersion = itemMetadata.getVersion();
      boolean existVersionInServer = (serverVersion >= clientVersion);

      if (existVersionInServer) {
         this.saveExistingVersion(serverItem, itemMetadata);
      } else {
         // Check if version is correct
         if (serverVersion + 1 == clientVersion) {
            this.saveNewVersion(itemMetadata, serverItem, workspace, device);
         } else {
            throw new CommitWrongVersion("Invalid version.", serverItem);
         }
      }
   }

   private void saveNewObject(ItemMetadataRMI metadata, WorkspaceRMI workspace,
         DeviceRMI device) throws Exception {

      // Create workspace and parent instances
      Long parentId = metadata.getParentId();
      ItemRMI parent = null;
      if (parentId != null) {
         parent = itemDao.findById(parentId);
      }

      beginTransaction();

      if (metadata.getStatus()==null)
         metadata.setStatus(Status.NEW.toString());

      try {
         // Insert object to DB
         ItemRMI item = new ItemRMI(
               metadata.getId(),
               workspace,
               metadata.getVersion(),
               parent,
               null,
               metadata.getFilename(),
               metadata.getMimetype(),
               metadata.isFolder(),
               metadata.getParentVersion());

         workspace.add(item);
         itemDao.put(item);

         // Insert objectVersion

         ItemVersionRMI objectVersion = new ItemVersionRMI(
               random.nextLong(),
               item.getId(),
               device,
               metadata.getVersion(),
               metadata.getModifiedAt(), // FIXME
               metadata.getModifiedAt(),
               metadata.getChecksum(),
               metadata.getStatus(),
               metadata.getSize());

         item.addVersion(objectVersion);
         itemVersionDao.add(objectVersion);

         // If no folder, create new chunks
         if (!metadata.isFolder()) {
            List<String> chunks = metadata.getChunks();
            this.createChunks(chunks, objectVersion);
         }


         commitTransaction();
      } catch (Exception e) {
         e.printStackTrace();
         logger.error(e);
         rollbackTransaction();
      }
   }

   private void saveNewVersion(ItemMetadataRMI metadata, ItemRMI serverItem,
         WorkspaceRMI workspace, DeviceRMI device) throws Exception {

      beginTransaction();

      try {
         // Create new objectVersion
         ItemVersionRMI itemVersion = new ItemVersionRMI(
               metadata.getId(),
               serverItem.getId(),
               device,
               metadata.getVersion(),
               Date.from(Instant.now()),
               metadata.getModifiedAt(),
               metadata.getChecksum(),
               metadata.getStatus(),
               metadata.getSize());
         itemVersionDao.add(itemVersion);

         // If no folder, create new chunks
         if (!metadata.isFolder()) {
            List<String> chunks = metadata.getChunks();
            this.createChunks(chunks, itemVersion);
         }

         // TODO To Test!!
         String status = metadata.getStatus();
         if (status.equals(Status.RENAMED.toString())
               || status.equals(Status.MOVED.toString())
               || status.equals(Status.DELETED.toString())) {

            serverItem.setFilename(metadata.getFilename());

            Long parentFileId = metadata.getParentId();
            if (parentFileId == null) {
               serverItem.setClientParentFileVersion(null);
               serverItem.setParent(null);
            } else {
               serverItem.setClientParentFileVersion(metadata
                     .getParentVersion());
               ItemRMI parent = itemDao.findById(parentFileId);
               serverItem.setParent(parent);
            }
         }

         // Update object latest version
         serverItem.setLatestVersionNumber(metadata.getVersion());
         itemDao.put(serverItem);

         commitTransaction();
      } catch (Exception e) {
         logger.error(e);
         rollbackTransaction();
      }
   }

   private void createChunks(List<String> chunksString,
         ItemVersionRMI objectVersion) throws Exception {
      if (chunksString != null) {
         if (chunksString.size() > 0) {
            List<ChunkRMI> chunks = new ArrayList<ChunkRMI>();
            int i = 0;
            for (String chunkName : chunksString) {
               chunks.add(new ChunkRMI(chunkName, i));
               i++;
            }
            itemVersionDao.insertChunks(objectVersion.getItemId(), chunks, objectVersion.getId());
         }
      }
   }

   private void saveExistingVersion(ItemRMI serverObject,
         ItemMetadataRMI clientMetadata) throws CommitWrongVersion,
         CommitExistantVersion, Exception {

      ItemMetadataRMI serverMetadata = this.getServerObjectVersion(serverObject,
            clientMetadata.getVersion());

      if (!clientMetadata.equals(serverMetadata)) {
         throw new CommitWrongVersion("Invalid version.", serverObject);
      }

      boolean lastVersion = (serverObject.getLatestVersion()
            .equals(clientMetadata.getVersion()));

      if (!lastVersion) {
         throw new CommitExistantVersion("This version already exists.",
               serverObject, clientMetadata.getVersion());
      }
   }

   private ItemMetadataRMI getCurrentServerVersion(ItemRMI serverObject)
         throws Exception {
      return getServerObjectVersion(serverObject,
            serverObject.getLatestVersionNumber());
   }

   private ItemMetadataRMI getServerObjectVersion(ItemRMI serverObject,
         long requestedVersion) throws Exception {

      ItemMetadataRMI metadata = itemVersionDao.findByItemIdAndVersion(
            serverObject.getId(), requestedVersion);

      return metadata;
   }

   private void beginTransaction() throws Exception {
      try {
         connection.setAutoCommit(false);
      } catch (Exception e) {
         throw new Exception(e);
      }
   }

   private void commitTransaction() throws Exception {
      try {
         connection.commit();
         this.connection.setAutoCommit(true);
      } catch (Exception e) {
         throw new Exception(e);
      }
   }

   private void rollbackTransaction() throws Exception {
      try {
         this.connection.rollback();
         this.connection.setAutoCommit(true);
      } catch (Exception e) {
         throw new Exception(e);
      }
   }
}
