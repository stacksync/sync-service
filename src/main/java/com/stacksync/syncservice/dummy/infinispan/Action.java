/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class Action {

    private String type;
    private Long tempId;

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
    
    protected Float timestamp;
    protected String op, file_type, file_mime;
    protected Long file_id;
    protected Integer file_size;
    protected UUID user_id;

    public Action(String[] lineParts) {
        this.timestamp = Float.parseFloat(lineParts[0]);
        this.op = lineParts[1];
        this.file_id = Long.parseLong(lineParts[2]);
        this.file_type = lineParts[3];
        this.file_mime = lineParts[4];
        this.file_size = Integer.parseInt(lineParts[5]);
        this.user_id = UUID.fromString(lineParts[6]);
    }

    public String getType() {
        return type;
    }

    public Long getTempId() {
        return tempId;
    }

    public UUID getUser_id() {
        return user_id;
    }

    protected ItemMetadata createItemMetadata(Random ran, int min, int max, UUID uuid, Long id, String filename) {
        return null;
    }

    protected void setValues(Random ran, Long id, Long version, Long parentId, Long parentVersion, Long checksum, List<String> chunks, Boolean isFolder, String filename, int numChunks, long size) {
        String[] mimes = {"pdf", "php", "java", "docx", "html", "png", "jpeg", "xml"};

        this.id = id;
        this.version = version;

        this.parentId = parentId;
        this.parentVersion = parentVersion;

        this.status = type;
        this.modifiedAt = new Date();
        this.checksum = (long) ran.nextInt(Integer.MAX_VALUE);
        this.chunks = chunks;
        this.isFolder = isFolder;
        this.filename = filename;
        if (this.mimetype == null) {
            this.mimetype = mimes[ran.nextInt(mimes.length)];
        }

        // Fill chunks
        this.numChunks = numChunks;
        this.size = size;
    }

    protected String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

    }

}
