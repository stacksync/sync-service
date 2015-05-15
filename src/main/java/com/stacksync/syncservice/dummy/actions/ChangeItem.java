/**
 * 
 */
package com.stacksync.syncservice.dummy.actions;

import java.util.UUID;

import com.stacksync.syncservice.handler.Handler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class ChangeItem extends Action {

	public ChangeItem(Handler handler, UUID userId, Long fileId, Long fileSize, String fileType, String fileMime, Long fileVersion) {
		super(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
		status = "CHANGED";
	}
}
