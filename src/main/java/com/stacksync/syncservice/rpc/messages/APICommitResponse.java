package com.stacksync.syncservice.rpc.messages;

import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.ItemMetadata;

public class APICommitResponse extends APIResponse {

    public APICommitResponse(ItemMetadata item, Boolean success, int error, String description) {
        this.success = success;
        this.errorCode = error;
        this.description = description;
        if (item != null) {
            this.item = new CommitInfo(item.getVersion(),
                    success, item);
        }
    }
}
