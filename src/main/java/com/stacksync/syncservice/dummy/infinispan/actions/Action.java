package com.stacksync.syncservice.dummy.infinispan.actions;

import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemMetadataRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.handler.Handler;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public abstract class Action {

    protected final Logger logger = Logger.getLogger(Action.class.getName());
    protected static final double CHUNK_SIZE = 512 * 1024;
    protected Handler handler;
    protected UUID userId;
    protected Long fileId, fileSize, fileVersion;
    protected String status, fileType, fileMime;

    public Action(Handler handler, UUID userId, Long fileId, Long fileSize, String fileType, String fileMime, Long fileVersion) {
        this.handler = handler;
        this.userId = userId;
        this.fileId = fileId;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.fileMime = fileMime;
        this.fileVersion = fileVersion;
    }

    protected String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

    }

    protected ItemMetadataRMI createItemMetadata(Random ran) {

        Date modifiedAt = new Date();
        Long checksum = (long) ran.nextInt(Integer.MAX_VALUE);
        List<String> chunks = new ArrayList<String>();
        int numChunks = (int) Math.ceil(fileSize / CHUNK_SIZE);

        for (int i = 0; i < numChunks; i++) {
            String str = java.util.UUID.randomUUID().toString();
            try {
                chunks.add(doHash(str));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        ItemMetadataRMI itemMetadata = new ItemMetadataRMI(fileId, fileVersion, userId, null, null, status, modifiedAt, checksum, fileSize,
                false, fileId.toString(), fileMime, chunks);
        itemMetadata.setChunks(chunks);

        return itemMetadata;

    }

    public void doCommit() throws Exception {
        String idCommit = java.util.UUID.randomUUID().toString();

        UserRMI user = new UserRMI(userId);
        DeviceRMI device = new DeviceRMI(userId,"android",user);
        WorkspaceRMI workspace = new WorkspaceRMI(userId);

        Random ran = new Random(System.currentTimeMillis());
        List<ItemMetadataRMI> items = new ArrayList<>();
        items.add(createItemMetadata(ran));
        logger.info("hander_doCommit_start,commitID=" + idCommit);
        handler.doCommit(user, workspace, device, items);
        logger.info("hander_doCommit_end,commitID=" + idCommit);

    }
}
