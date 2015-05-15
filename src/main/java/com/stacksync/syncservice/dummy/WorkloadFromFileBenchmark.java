/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.dummy.actions.Action;
import com.stacksync.syncservice.dummy.actions.ActionFactory;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class WorkloadFromFileBenchmark {
	// timestamp,op,file_id,file_type,file_mime,file_size,file_version,sid,user_id
	public static void main(String[] args) throws Exception {

		args = new String[] { "trace.txt" };

		if (args.length != 1) {
			System.err.println("Usage: file_path");
			System.exit(0);
		}

		String configPath = "config2.properties";
		Config.loadProperties(configPath);
		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		Handler handler = new SQLSyncHandler(pool);

		String line;
		BufferedReader buff = new BufferedReader(new FileReader(new File(args[0])));

		long execTime = 0L;

		long start = System.currentTimeMillis();
		line = buff.readLine();
		if (line != null) {
			do {

				String[] words = line.split(",");
				double timestamp = Double.parseDouble(words[0]);
				long t = (long) (timestamp * 1000);
				String op = words[1];
				Long fileId = Long.parseLong(words[2]);
				String fileType = words[3];
				String fileMime = words[4];
				Long fileSize = Long.parseLong(words[5]);
				Long fileVersion = Long.parseLong(words[6]);
				UUID userId = UUID.fromString(words[8]);

				long sleep = t - execTime;
				if (sleep > 0) {
					Thread.sleep(sleep);
				}

				Action action = ActionFactory.getNewAction(op, handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
				action.doCommit();

				long end = System.currentTimeMillis();

				execTime = end - start;

			} while ((line = buff.readLine()) != null);
		}

		buff.close();

	}
}