package com.stacksync.syncservice.test.benchmark.normal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.util.Config;
import org.infinispan.atomic.AtomicObjectFactoryRemoteTest;
import org.infinispan.atomic.utils.Server;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestCommit extends AtomicObjectFactoryRemoteTest{

   private final static int NUMBER_TASKS = 1;
   private final static int NUMBER_COMMITS = 1000;

   public int getReplicationFactor() {
      return 2;
   }

   public int getNumberOfManagers() {
      return 2;
   }

   @Test
   public void commit() throws Exception{
      ExecutorService service = Executors.newFixedThreadPool(NUMBER_TASKS);

      Config.loadProperties();
      String datasource = Config.getDatasource();
      ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
      pool.getConnection().cleanup();
      Handler handler = new SQLSyncHandler(pool);

      List<Future> futures = new ArrayList<>();
      for (int i=0; i<NUMBER_TASKS; i++) {
         CommitTask task = new CommitTask(handler);
         futures.add(service.submit(task));
      }

      for(Future future: futures) {
         future.get();
      }

      pool.getConnection().close();

   }

   public static List<ItemMetadataRMI> getObjectMetadata(JsonArray allFiles) {
		List<ItemMetadataRMI> metadataList = new ArrayList<>();

		for (int i = 0; i < allFiles.size(); i++) {
			JsonObject file = allFiles.get(i).getAsJsonObject();

			long fileId = file.get("file_id").getAsLong();
			long version = file.get("version").getAsLong();

			Long parentFileVersion = null;
			try {
				parentFileVersion = file.get("parent_file_version").getAsLong();
			} catch (Exception ex) {
				// ex.printStackTrace();
			}

			Long parentFileId = null;
			try {
				parentFileId = file.get("parent_file_id").getAsLong();
			} catch (Exception ex) {
				// ex.printStackTrace();
			}

			Date updated = new Date(file.get("updated").getAsLong());
			String status = file.get("status").getAsString();
			Date lastModified = new Date(file.get("lastModified").getAsLong());
			long checksum = file.get("checksum").getAsLong();
			long fileSize = file.get("fileSize").getAsLong();

			int folderInt = file.get("folder").getAsInt();
			boolean folder = folderInt == 0 ? false : true;

			String name = file.get("name").getAsString();
			String mimetype = file.get("mimetype").getAsString();
			JsonArray jChunks = file.get("chunks").getAsJsonArray(); // more
																		// optimal
			List<String> chunks = new ArrayList<String>();
			for (int j = 0; j < jChunks.size(); j++) {
				chunks.add(jChunks.get(j).getAsString());
			}
			
			ItemMetadataRMI object = new ItemMetadataRMI(
               fileId, version, Constants.DEVICE_ID, parentFileId, parentFileVersion, status, lastModified,
					checksum, fileSize, folder, name, mimetype, chunks);

			metadataList.add(object);
		}

		return metadataList;
	}

	public static void main(String[] args) throws Exception {
      ExecutorService service = Executors.newFixedThreadPool(2);
      Server server1 = new Server("127.0.0.1:11222","127.0.0.1:11222",1,false);
      service.submit(server1);
      server1.waitLaunching();

      Config.loadProperties();
      String datasource = Config.getDatasource();
      ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
      pool.getConnection().cleanup();
      Handler handler = new SQLSyncHandler(pool);

      long startTotal = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++) {
         try {
            String metadata = CommonFunctions.generateObjects(1, Constants.DEVICE_ID);
            JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
            List<ItemMetadataRMI> objects = getObjectMetadata(rawObjects);
            UserRMI user = new UserRMI(UUID.randomUUID());
            user.setId(Constants.USER);
            DeviceRMI device = new DeviceRMI(Constants.DEVICE_ID, "", user);
            WorkspaceRMI workspace = new WorkspaceRMI(Constants.WORKSPACE_ID);
            handler.doCommit(user, workspace, device, objects);
         } catch (DAOException e) {
            e.printStackTrace();
         }
      }

      long totalTime = System.currentTimeMillis() - startTotal;

      System.out.println("Total level time --> " + totalTime + " ms");

      pool.getConnection().close();
      service.shutdown();
      System.exit(0);
	}

   private class CommitTask implements Runnable{

      private Handler handler;

      public CommitTask(Handler handler) {
         this.handler = handler;
      }

      @Override
      public void run() {
         try {
            List<WorkspaceRMI> workspaces = new ArrayList<>();
            long startTotal = System.currentTimeMillis();
            for (int i = 0; i < NUMBER_COMMITS; i++) {
               try {
                  String metadata = CommonFunctions.generateObjects(1, UUID.randomUUID());
                  JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
                  List<ItemMetadataRMI> objects = getObjectMetadata(rawObjects);
                  UserRMI user = new UserRMI(UUID.randomUUID());
                  DeviceRMI device = new DeviceRMI(UUID.randomUUID(), "android", user);
                  WorkspaceRMI workspace = new WorkspaceRMI(UUID.randomUUID(), 1, user, false, false);
                  handler.doCommit(user, workspace, device, objects);
                  workspaces.add(workspace);
               } catch (DAOException e) {
                  e.printStackTrace();
               }
            }
            long totalTime = System.currentTimeMillis() - startTotal;

            System.out.println("Total level time --> " + totalTime + " ms");

         } catch (Exception e) {
            e.printStackTrace();
         }
      }

   }

}
