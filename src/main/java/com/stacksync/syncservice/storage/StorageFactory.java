package com.stacksync.syncservice.storage;

import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;
import com.stacksync.syncservice.storage.StorageManager.StorageType;

public class StorageFactory {
	
	public static StorageManager getStorageManager(StorageType type) throws NoStorageManagerAvailable{
		
		if (type == StorageType.SWIFT) {
			return SwiftManager.getInstance();
		} else if (type == StorageType.DUMMY) {
			return new DummyStorage();
		}
		
		throw new NoStorageManagerAvailable(String.format("Storage type '%s' not found", type));
	}
}
