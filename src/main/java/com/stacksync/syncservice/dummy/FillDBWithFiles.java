/**
 * 
 */
package com.stacksync.syncservice.dummy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.dummy.actions.NewItem;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.util.Config;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class FillDBWithFiles {
	// file_id,user_id

	protected ConnectionPool pool;
	protected Handler handler;

	public FillDBWithFiles() throws Exception {
		String configPath = "config.properties";
		Config.loadProperties(configPath);
		String datasource = Config.getDatasource();
		pool = ConnectionPoolFactory.getConnectionPool(datasource);
		handler = new SQLSyncHandler(pool);
	}

	public void doCommit(UUID userId, long fileId) throws DAOException {
		NewItem newItem = new NewItem(handler, userId, fileId, null, null, null, 1L);
		newItem.doCommit();
	}

	public static void main(String[] args) throws Exception {
		args = new String[] { "day_files_without_new.csv" };

		if (args.length != 1) {
			System.err.println("Usage: file_path");
			System.exit(0);
		}

		FillDBWithFiles filler = new FillDBWithFiles();

		String line;
		BufferedReader buff = new BufferedReader(new FileReader(new File(args[0])));
		while ((line = buff.readLine()) != null) {
			try {
				String[] words = line.split(",");
				Long itemId = Long.parseLong(words[0]);
				UUID userId = UUID.fromString(words[1]);

				filler.doCommit(userId, itemId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		buff.close();
	}

}
