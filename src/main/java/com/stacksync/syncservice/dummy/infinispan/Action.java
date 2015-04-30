/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import static com.stacksync.syncservice.dummy.infinispan.AReadFile.CHUNK_SIZE;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public abstract class Action {

    protected HashMap<Long, Long> itemMetadataID;

    protected Long id;
    protected Long version;

    protected Long parentId;
    protected Long parentVersion;

    protected String status;
    protected Date modifiedAt;
    protected Long checksum;
    protected List<String> chunks;
    protected Boolean isFolder;
    protected String filename;
    protected String mimetype;
    protected int numChunks;
    protected long size;

    protected Long timestamp;
    protected String fileType, fileMime;
    protected Long fileId;
    protected Integer fileSize;
    protected Long userId;

    private Random ran;
    private int max, min;

    public Action(String[] lineParts) {
        this.ran = new Random(System.currentTimeMillis());
        this.min = 1;
        this.max = 8;

        Float tstamp = Float.parseFloat(lineParts[0])*1000;
        this.timestamp = tstamp.longValue();
        this.status = lineParts[1];
        this.fileId = Long.parseLong(lineParts[2]);
        this.fileType = lineParts[3];
        this.fileMime = lineParts[4];
        if (lineParts[5].equals("")) {
            this.fileSize = 0;
        } else {
            this.fileSize = Integer.parseInt(lineParts[5]);
        }
        this.userId = Long.parseLong(lineParts[6]);
        this.version = Long.parseLong(lineParts[7]);
    }

    public String getStatus() {
        return status;
    }

    public Long getTempId() {
        return fileId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public abstract ItemMetadata createItemMetadata(UUID uuid, Long id, String filename);

    public void setValues(Long id, Long parentId, Long parentVersion, Boolean isFolder, String filename, Boolean getChunks) {
        String[] mimes = {"pdf", "php", "java", "docx", "html", "png", "jpeg", "xml"};

        this.id = id;

        this.parentId = parentId;
        this.parentVersion = parentVersion;

        this.modifiedAt = new Date();
        this.checksum = (long) ran.nextInt(Integer.MAX_VALUE);
        this.isFolder = isFolder;
        this.filename = filename;
        if (this.mimetype == null) {
            this.mimetype = mimes[ran.nextInt(mimes.length)];
        }

        // Fill chunks
        if (getChunks) {
            this.chunks = new ArrayList<String>();
            this.numChunks = ran.nextInt((max - min) + 1) + min;
            this.size = numChunks * CHUNK_SIZE;
            for (int i = 0; i < numChunks; i++) {
                try {
                    String str = java.util.UUID.randomUUID().toString();
                    this.chunks.add(this.doHash(str));
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(Action.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(Action.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        // TODO autogenerate hash
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

    }

}
