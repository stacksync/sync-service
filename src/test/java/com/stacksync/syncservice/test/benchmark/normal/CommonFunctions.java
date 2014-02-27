package com.stacksync.syncservice.test.benchmark.normal;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class CommonFunctions {

	public class ParentRoot {
		public String parentRootId;
		public Long fileVersion;
		public Long fileId;

		public ParentRoot(String parentRootId, Long fileVersion, Long fileId) {
			this.parentRootId = parentRootId;
			this.fileVersion = fileVersion;
			this.fileId = fileId;
		}
	}

	private static JsonArray generateChunks(int size) {
		JsonArray jArray = new JsonArray();
		RandomString strRandom = new RandomString(20);

		for (int i = 0; i < size; i++) {
			JsonElement elem = new JsonPrimitive(strRandom.nextString());
			jArray.add(elem);
		}

		return jArray;
	}

	private static JsonArray generateObjectsLevel(int numObjects, UUID deviceId, ParentRoot parentRoot) {
		JsonArray arrayObjects = new JsonArray();

		Random random = new Random();
		RandomString strRandom = new RandomString(10);
		for (int i = 0; i < numObjects; i++) {
			JsonObject file = new JsonObject();

			file.addProperty("file_id", random.nextLong());
			file.addProperty("version", new Long(1));

			if (parentRoot != null) {
				file.addProperty("parent_file_version", parentRoot.fileVersion);
				file.addProperty("parent_file_id", parentRoot.fileId);
			} else {
				file.addProperty("parent_file_version", "");
				file.addProperty("parent_file_id", "");
			}

			Date date = new Date();
			file.addProperty("updated", date.getTime());

			file.addProperty("status", "NEW");
			file.addProperty("lastModified", date.getTime());

			file.addProperty("checksum", random.nextLong());
			file.addProperty("clientName", deviceId.toString());

			file.addProperty("fileSize", random.nextLong());

			file.addProperty("folder", 0);
			file.addProperty("name", strRandom.nextString());

			file.addProperty("path", strRandom.nextString());

			file.addProperty("mimetype", "Text");
			file.add("chunks", generateChunks(10));

			arrayObjects.add(file);
		}

		return arrayObjects;
	}

	public static String generateObjects(int numObjects, UUID deviceId) {
		return CommonFunctions.generateObjectsLevel(numObjects, deviceId, null).toString();
	}
}
