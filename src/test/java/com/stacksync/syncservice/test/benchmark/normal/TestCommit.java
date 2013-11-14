package com.stacksync.syncservice.test.benchmark.normal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.rpc.messages.Commit;
import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.util.Config;

public class TestCommit {

	public static List<ObjectMetadata> getObjectMetadata(JsonArray allFiles) {
		List<ObjectMetadata> metadataList = new ArrayList<ObjectMetadata>();

		for (int i = 0; i < allFiles.size(); i++) {
			JsonObject file = allFiles.get(i).getAsJsonObject();

			String rootId = file.get("root_id").getAsString();
			long fileId = file.get("file_id").getAsLong();
			long version = file.get("version").getAsLong();
			String parentRootId = file.get("parent_root_id").getAsString();

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
			String clientName = file.get("clientName").getAsString();
			long fileSize = file.get("fileSize").getAsLong();

			int folderInt = file.get("folder").getAsInt();
			boolean folder = folderInt == 0 ? false : true;

			String name = file.get("name").getAsString();
			String path = file.get("path").getAsString();
			String mimetype = file.get("mimetype").getAsString();
			JsonArray jChunks = file.get("chunks").getAsJsonArray(); // more
																		// optimal
			List<String> chunks = new ArrayList<String>();
			for (int j = 0; j < jChunks.size(); j++) {
				chunks.add(jChunks.get(j).getAsString());
			}

			ObjectMetadata object = new ObjectMetadata(rootId, fileId, version, parentRootId, parentFileId, parentFileVersion, updated, status, lastModified,
					checksum, clientName, chunks, fileSize, folder, name, path, mimetype);

			metadataList.add(object);
		}

		return metadataList;
	}

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		Handler handler = new SQLHandler(pool);

		String metadata = CommonFunctions.generateObjects(1, Constants.DEVICENAME);
		long startTotal = System.currentTimeMillis();

		JsonArray rawObjects = new JsonParser().parse(metadata).getAsJsonArray();
		List<ObjectMetadata> objects = getObjectMetadata(rawObjects);
		Commit commitRequest = new Commit(Constants.USER, Constants.REQUESTID, objects, Constants.DEVICENAME, Constants.WORKSPACEID);

		handler.doCommit(commitRequest);

		long totalTime = System.currentTimeMillis() - startTotal;
		// System.out.println("Objects -> " + ((GetChangesResponseMessage)
		// response).getMetadata().size());
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
