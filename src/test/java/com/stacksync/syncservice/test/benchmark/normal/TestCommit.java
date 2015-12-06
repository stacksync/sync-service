package com.stacksync.syncservice.test.benchmark.normal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestCommit {

   private final static int DEFAULT_NUMBER_TASKS = 1;
   private final static int DEFAULT_NUMBER_COMMITS = 1;
   private final static int DEFAULT_NUMBER_USERS = 4000;

   private static final String defaultServer ="localhost:11222";

   @Option(name = "-server", usage = "ip:port or ip of the server")
   private String server = defaultServer;

   @Option(name = "-tasks", usage = "number of tasks; default="+DEFAULT_NUMBER_TASKS)
   private int nNumberTasks = DEFAULT_NUMBER_TASKS;

   @Option(name = "-commits", usage = "number of commits per task; default="+DEFAULT_NUMBER_COMMITS)
   private int numberCommits = DEFAULT_NUMBER_COMMITS;

   @Option(name = "-users", usage = "number of users; default="+DEFAULT_NUMBER_USERS)
   private int numberUsers = DEFAULT_NUMBER_USERS;

   @Option(name = "-verbose", usage = "print time for each operation; default=false")
   private boolean verbose = false;

   @Option(name = "-load", usage = "load phase; default=false")
   private boolean load = false;

   @Option(name = "-validate", usage = "validate content after execution; default=false")
   private boolean validate = false;

   @Option(name = "-content", usage = "list content; default=false")
   private boolean content= false;

   private List<UUID> users;

   public static void main(String[] args) {
      new TestCommit().doMain(args);
   }

   private void doMain(String[] args) {
      CmdLineParser parser = new CmdLineParser(this);
      parser.setUsageWidth(80);
      try {
         parser.parseArgument(args);
         if (content && load)
            throw  new CmdLineException("");
      } catch( CmdLineException e ) {
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
         System.err.println();
         return;
      }

      try {

         Config.loadProperties();
         Properties properties = Config.getProperties();
         properties.setProperty("infinispan_host", server);
         String datasource = Config.getDatasource();
         ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);

         createUsers();

         if (content) {
            listContent(pool);
         } else {
            if (load) {
               pool.getConnection().cleanup();
               populate(pool);
            } else {
               execute(pool, validate);
            }
         }

         pool.getConnection().close();

      } catch (Exception e) {
         e.printStackTrace();
      }


      System.exit(0);

   }

   public TestCommit(){}

   public TestCommit(int numberTasks, int numberCommits, int numberUsers, boolean verbose){
      this.numberCommits = numberCommits;
      this.nNumberTasks = numberTasks;
      this.numberUsers = numberUsers;
      this.verbose = verbose;
   }

   public void createUsers() {
      users  = new ArrayList<>(numberUsers);
      for(int i=0; i < numberUsers; i++) {
         UUID userId = UUID.nameUUIDFromBytes(("cli" + Integer.toString(i)).getBytes());
         users.add(userId);
      }
   }

   public void listContent(ConnectionPool pool) {
      System.out.println("Displaying content (" + numberUsers + " users)");
      try {
         Handler handler = new SQLSyncHandler(pool);
         for (UUID uuid : users) {
            WorkspaceRMI workspace = handler.getWorkspace(uuid);
            System.out.println(workspace.content());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   public void populate(ConnectionPool pool) throws Exception {
      Handler handler = new SQLSyncHandler(pool);
      System.out.print("Creating " + numberUsers + " users ... ");

      // populate
      for(UUID userId : users) {
         handler.populate(userId);
      }

      System.out.println("done");

   }

   public void execute(ConnectionPool pool, boolean validate) throws Exception {

      System.out.println("Executing (" + numberUsers + " users)");

      // we launch the tasks
      ExecutorService service = Executors.newFixedThreadPool(nNumberTasks);
      long start = System.currentTimeMillis();
      List<Future<Float>> futures = new ArrayList<>();
      for (int i=0; i< nNumberTasks; i++) {
         Handler handler = new SQLSyncHandler(pool); // new handler for each task
         CommitTask task = new CommitTask(handler, numberCommits, users, verbose);
         futures.add(service.submit(task));
      }

      float totalThroughput = 0;
      for(Future<Float> future: futures) {
         totalThroughput += future.get();
      }
      System.out.println("TotalTime=" + (System.currentTimeMillis() - start));
      System.out.println("TotalThroughput=" + totalThroughput);

      if (validate) {
         Handler handler = new SQLSyncHandler(pool);
         int totalItems = 0;
         for (UUID uuid : users) {
            WorkspaceRMI workspace = handler.getWorkspace(uuid);
            totalItems += workspace.getItems().size();
         }
         assert totalItems == nNumberTasks * numberCommits : totalItems + "!=" + nNumberTasks * numberCommits;
      }
   }


   // Helpers

   public static List<ItemMetadataRMI> getObjectMetadata(JsonArray allFiles, UUID deviceId) {
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
               fileId, version, deviceId, parentFileId, parentFileVersion, status, lastModified,
					checksum, fileSize, folder, name, mimetype, chunks);

			metadataList.add(object);
		}

		return metadataList;
	}

}
