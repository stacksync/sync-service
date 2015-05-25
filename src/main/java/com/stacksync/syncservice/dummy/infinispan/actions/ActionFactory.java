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
public class ActionFactory {

    public static Action getNewAction(String option, Handler handler, UUID userId, Long fileId, Long fileSize, String fileType,
            String fileMime, Long fileVersion) {
        Action action = null;
        if ("new".equalsIgnoreCase(option)) {
            action = new NewItem(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
        } else if ("mod".equalsIgnoreCase(option)) {
            action = new ChangeItem(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
        } else if ("mov".equalsIgnoreCase(option)) {
            action = new RenameItem(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
        } else if ("del".equalsIgnoreCase(option)) {
            action = new DeleteItem(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
        }

        return action;

    }
}
