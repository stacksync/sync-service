/**
 * 
 */
package com.stacksync.syncservice.dummy.server;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.exceptions.storage.NoStorageManagerAvailable;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class ServerDummy extends AServerDummy {

	private UUID userID;

	public ServerDummy(ConnectionPool pool, UUID userID, int commitsPerMinute, int minutes) throws SQLException,
			NoStorageManagerAvailable {
		super(pool, commitsPerMinute, minutes);
		this.userID = userID;
	}

	@Override
	public void run() {
		Random ran = new Random(System.currentTimeMillis());
		// Distance between commits in msecs
		long distance = (long) (1000 / (commitsPerMinute / 60.0));

		// Every iteration takes a minute
		for (int i = 0; i < minutes; i++) {

			long startMinute = System.currentTimeMillis();
			for (int j = 0; j < commitsPerMinute; j++) {
				String id = UUID.randomUUID().toString();

				logger.info("serverDummy_doCommit_start,commitID=" + id);
				long start = System.currentTimeMillis();
				try {
					doCommit(userID, ran, 1, 8, id);
				} catch (DAOException e1) {
					logger.error(e1);
				}
				long end = System.currentTimeMillis();
				logger.info("serverDummy_doCommit_end,commitID=" + id);
				
				// If doCommit had no cost sleep would be distance but we have
				// to take into account of the time that it takes
				long sleep = distance - (end - start);
				if (sleep > 0) {
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			long endMinute = System.currentTimeMillis();
			long minute = endMinute - startMinute;

			// I will forgive 5 seconds of delay...
			if (minute > 65 * 1000) {
				// Notify error
				logger.error("MORE THAN 65 SECONDS=" + (minute / 1000));
			}
		}
	}
}
