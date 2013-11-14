package com.stacksync.syncservice.test.benchmark;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.stacksync.syncservice.model.Chunk;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.model.ObjectVersion;
import com.stacksync.syncservice.model.Workspace;

/**
 * 
 * @author cotes
 * @author gguerrero
 */
public class MetadataGenerator {
	private static final int totalPercentage = 100;
	private static final int folderPercentage = 40;

	private static final int MAX_VERSIONS = 5;
	private static final int MAX_CHUNKS = 100;
	private static final int MAX_FILES_LEVEL = 100;

	private Random randGenerator;

	public MetadataGenerator() {
		this.randGenerator = new Random();
	}

	public ArrayList<Object1> generateLevel(Workspace workspace, Device device, Object1 parent) {
		int objectsLevel = randGenerator.nextInt(MAX_FILES_LEVEL);
		ArrayList<Object1> objects = new ArrayList<Object1>();

		for (int i = 0; i < objectsLevel; i++) {
			Object1 currentObject;
			int folderValue = randGenerator.nextInt(totalPercentage);
			boolean folder = false;

			if (folderValue < folderPercentage) {
				folder = true;
			}

			currentObject = this.generateMetadata(workspace, device, parent, folder);
			objects.add(currentObject);
		}

		return objects;
	}

	private Object1 generateMetadata(Workspace workspace, Device device, Object1 parent, boolean folder) {
		long numVersions = randGenerator.nextInt(MAX_VERSIONS);
		if (numVersions == 0) {
			numVersions = 1;
		}

		int numChunks = randGenerator.nextInt(MAX_CHUNKS);
		if (numChunks == 0) {
			numChunks = 1;
		}

		Object1 object = new Object1();
		object.setRootId("stacksync");
		object.setWorkspace(workspace);
		object.setLatestVersion(numVersions);
		object.setParent(parent);
		object.setClientFileId((long) randGenerator.nextInt()); // If nextLong
		// fails!
		object.setClientFileName(randomString());
		object.setClientFileMimetype("Document");
		object.setClientFolder(folder);

		if (parent == null) {
			object.setClientParentRootId("");
			object.setClientParentFileId(null);
			object.setClientParentFileVersion(null);
		} else {
			object.setClientParentRootId(parent.getRootId());
			object.setClientParentFileId(parent.getClientFileId());
			object.setClientParentFileVersion(parent.getLatestVersion());
		}

		List<ObjectVersion> versions = new ArrayList<ObjectVersion>();
		for (int i = 0; i < numVersions; i++) {
			ObjectVersion version = new ObjectVersion();
			version.setObject(object);
			version.setDevice(device);
			version.setVersion(i + 1L);
			version.setServerDateModified(new Date());
			version.setChecksum(randGenerator.nextLong());
			version.setClientDateModified(new Date());

			if (i == 0) {
				version.setClientStatus("NEW");
			} else {
				version.setClientStatus("CHANGED"); // TODO improve with random
													// (DELETED, RENA..)
			}

			version.setClientFileSize((long) randGenerator.nextInt(10000));
			version.setClientName("name");
			version.setClientFilePath("/");

			List<Chunk> chunks = new ArrayList<Chunk>();
			if (!folder) {
				for (int j = 0; j < numChunks; j++) {
					Chunk chunk = new Chunk();
					chunk.setClientChunkName(randomString());
					chunks.add(chunk);
				}
			}
			version.setChunks(chunks);
			versions.add(version);
		}
		object.setVersions(versions);

		//System.out.println("Object Metadata -> " + object);
		return object;
	}

	protected String randomString() {
		return new BigInteger(130, this.randGenerator).toString(32);
	}

}
