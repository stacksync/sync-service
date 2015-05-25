/**
 *
 */
package com.stacksync.syncservice.dummy.infinispan.actions;

import java.util.UUID;

import com.stacksync.syncservice.handler.Handler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class RenameItem extends Action {

    public RenameItem(Handler handler, UUID userId, Long fileId, Long fileSize, String fileType, String fileMime, Long fileVersion) {
        super(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
        status = "RENAMED";
    }
}