package com.stacksync.syncservice.test.benchmark.normal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.SyncMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.util.Config;

public class TestCommit {

	public static List<SyncMetadata> getObjectMetadata(JsonArray allFiles) {
		List<SyncMetadata> metadataList = new ArrayList<SyncMetadata>();

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
			
			ItemMetadata object = new ItemMetadata(fileId, version, Constants.DEVICE_ID, parentFileId, parentFileVersion, status, lastModified,
					checksum, fileSize, folder, name, mimetype, chunks);

			metadataList.add(object);
		}

		return metadataList;
	}

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		Handler handler = new SQLSyncHandler(pool);

		String metadata = CommonFunctions.generateObjects(1, Constants.DEVICE_ID);
		long startTotal = System.currentTimeMillis();

		JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
		List<SyncMetadata> objects = getObjectMetadata(rawObjects);

		User user = new User();
		user.setId(Constants.USER);
		Device device = new Device( Constants.DEVICE_ID);
		Workspace workspace = new Workspace(Constants.WORKSPACE_ID);

		handler.doCommit(user, workspace, device, objects);

		long totalTime = System.currentTimeMillis() - startTotal;
		// System.out.println("Objects -> " + ((GetChangesResponseMessage)
		// response).getMetadata().size());
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
