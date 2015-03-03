package com.stacksync.syncservice.test.benchmark;

import com.stacksync.syncservice.db.infinispan.models.ChunkRMI;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemVersionRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

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

	public ArrayList<ItemRMI> generateLevel(WorkspaceRMI workspace, DeviceRMI device, ItemRMI parent) {
		int objectsLevel = randGenerator.nextInt(MAX_FILES_LEVEL);
		ArrayList<ItemRMI> objects = new ArrayList<ItemRMI>();

		for (int i = 0; i < objectsLevel; i++) {
			ItemRMI currentObject;
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

	private ItemRMI generateMetadata(WorkspaceRMI workspace, DeviceRMI device, ItemRMI parent, boolean folder) {
		long numVersions = randGenerator.nextInt(MAX_VERSIONS);
		if (numVersions == 0) {
			numVersions = 1;
		}

		int numChunks = randGenerator.nextInt(MAX_CHUNKS);
		if (numChunks == 0) {
			numChunks = 1;
		}

		ItemRMI item = new ItemRMI();
		//item.setWorkspace(workspace);
		item.setLatestVersionNumber(numVersions);
		item.setParent(parent);
		item.setId((long) randGenerator.nextInt()); // If nextLong
		// fails!
		item.setFilename(randomString());
		item.setMimetype("Document");
		item.setIsFolder(folder);

		if (parent == null) {
			item.setClientParentFileVersion(null);
		} else {
			item.setClientParentFileVersion(parent.getLatestVersionNumber());
		}

		List<ItemVersionRMI> versions = new ArrayList<ItemVersionRMI>();
		for (int i = 0; i < numVersions; i++) {
			ItemVersionRMI version = new ItemVersionRMI();
			version.setItem(item.getId());
			version.setDevice(device);
			version.setVersion(i + 1L);
			version.setModifiedAt(new Date());
			version.setChecksum(randGenerator.nextLong());

			if (i == 0) {
				version.setStatus("NEW");
			} else {
				version.setStatus("CHANGED"); // TODO improve with random
													// (DELETED, RENA..)
			}

			version.setSize((long) randGenerator.nextInt(10000));

			List<ChunkRMI> chunks = new ArrayList<ChunkRMI>();
			if (!folder) {
				for (int j = 0; j < numChunks; j++) {
					ChunkRMI chunk = new ChunkRMI();
					chunk.setClientChunkName(randomString());
					chunks.add(chunk);
				}
			}
			version.setChunks(chunks);
			versions.add(version);
		}
		item.setVersions(versions);

		//System.out.println("Object Metadata -> " + object);
		return item;
	}

	protected String randomString() {
		return new BigInteger(130, this.randGenerator).toString(32);
	}

}
