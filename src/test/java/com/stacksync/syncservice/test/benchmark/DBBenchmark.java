package com.stacksync.syncservice.test.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.Object1;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.test.benchmark.db.DatabaseHelper;

/**
 * 
 * @author cotes
 * @author gguerrero
 */
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

	public void fillDB(Workspace workspace, Device device) {
		int firstLevel = 0;

		try {
			this.createAndStoreMetadata(workspace, device, firstLevel, null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (DAOException e) {
			e.printStackTrace();
		}
	}

	public void createAndStoreMetadata(Workspace workspace, Device device, int currentLevel, Object1 parent)
			throws IllegalArgumentException, DAOException {

		if (currentLevel >= this.fsDepth) {
			return;
		}

		List<Object1> objectsLevel = metadataGen.generateLevel(workspace, device, parent);		
		this.dbHelper.storeObjects(objectsLevel);

		for (Object1 object : objectsLevel) {
			if (object.getClientFolder()) {
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
			User user = new User(null, name, cloudId, "email@email.com", 1000, 100);
			dbHelper.addUser(user);
			
			String clientWorkspaceName = name + "/";
			Workspace workspace = new Workspace(null, clientWorkspaceName, 1, user);
			dbHelper.addWorkspace(user, workspace);

			String deviceName = name + "_device";
			Device device = new Device(null, deviceName, user);
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
