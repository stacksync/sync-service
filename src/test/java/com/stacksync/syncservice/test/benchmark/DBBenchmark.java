package com.stacksync.syncservice.test.benchmark;

import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.test.benchmark.db.DatabaseHelper;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DBBenchmark extends Thread {

	private static final int LEVELS = 3;
	private static final int USERS = 30;

	private String name;
	private int numUser;
	private int fsDepth;
	private MetadataGenerator metadataGen;
	private DatabaseHelper dbHelper;


	public DBBenchmark(int numUser) throws Exception {
		super("TName" + numUser);
		
		this.name = "TName" + numUser;
		this.numUser = numUser;
		this.fsDepth = LEVELS;
		this.dbHelper = new DatabaseHelper();
		this.metadataGen = new MetadataGenerator();
	}

	public void fillDB(WorkspaceRMI workspace, DeviceRMI device) throws RemoteException {
		int firstLevel = 0;

		try {
			this.createAndStoreMetadata(workspace, device, firstLevel, null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (DAOException e) {
			e.printStackTrace();
		}
	}

	public void createAndStoreMetadata(WorkspaceRMI workspace, DeviceRMI device, int currentLevel, ItemRMI parent)
			throws IllegalArgumentException, DAOException, RemoteException {

		if (currentLevel >= this.fsDepth) {
			return;
		}

		List<ItemRMI> objectsLevel = metadataGen.generateLevel(workspace, device, parent);		
		this.dbHelper.storeObjects(objectsLevel);

		for (ItemRMI object : objectsLevel) {
			if (object.isFolder()) {
				createAndStoreMetadata(workspace, device, currentLevel + 1, object);
			}
		}

		System.out.println("***** " + name + " --> Stored objects: " + objectsLevel.size() + " at level: " + currentLevel);
	}
	
	@Override
	public void run(){
		Random randGenerator = new Random();
		System.out.println("============================");
		System.out.println("=========Thread " + name + "=========");
		System.out.println("============================");
		System.out.println("============================");

		System.out.println("Creating user: " + numUser);
		int randomUser = randGenerator.nextInt();
		String name = "benchmark" + randomUser;
		String cloudId = name;
		
		try {
			UserRMI user = new UserRMI(UUID.randomUUID(), "tester1", "tester1", "AUTH_12312312", "a@a.a", 100, 0);
			dbHelper.addUser(user);
			
			WorkspaceRMI workspace = new WorkspaceRMI(UUID.randomUUID(), 1, user.getId(), false, false);
			dbHelper.addWorkspace(user, workspace);

			String deviceName = name + "_device";
			DeviceRMI device = new DeviceRMI(UUID.randomUUID(), deviceName, user);
			dbHelper.addDevice(device);

			fillDB(workspace, device);

			System.out.println("User -> " + user);
			System.out.println("Workspace -> " + workspace);
			System.out.println("Device -> " + device);				
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException ex) {
                Logger.getLogger(DBBenchmark.class.getName()).log(Level.SEVERE, null, ex);
            }
	}

	

	public static void main(String[] args) {
		try {
			int numThreads = 1;
			List<DBBenchmark> benchmarks = new ArrayList<DBBenchmark>();
			for (int numUser = 0; numUser < USERS; numUser+=numThreads) {
				
				for(int i=0; i<numThreads; i++){					
					DBBenchmark benchmark = new DBBenchmark(numUser+i);
					benchmark.start();
					benchmarks.add(benchmark);
				}
				
				for(DBBenchmark benchmark: benchmarks){
					benchmark.join();
				}
				
				benchmarks.clear();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
