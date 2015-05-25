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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class TestUtil {
    
    private static final int CHUNK_SIZE = 512 * 1024;
    
    public static ItemMetadata createItemMetadata(UUID deviceId) {
        String[] mimes = {"pdf", "php", "java", "docx", "html", "png", "jpeg", "xml"};

        Random random = new Random();
        Long id = null;
        Long version = 1L;

        Long parentId = null;
        Long parentVersion = null;

        String status = "NEW";
        Date modifiedAt = new Date();
        Long checksum = (long) random.nextInt(Integer.MAX_VALUE);
        List<String> chunks = new ArrayList<String>();
        Boolean isFolder = false;
        String filename = java.util.UUID.randomUUID().toString();
        String mimetype = mimes[random.nextInt(mimes.length)];

        // Fill chunks
        int numChunks = random.nextInt(5) + 1;
        long size = numChunks * CHUNK_SIZE;
        for (int i = 0; i < numChunks; i++) {
            String str = java.util.UUID.randomUUID().toString();
            try {
                chunks.add(doHash(str));
            } catch (UnsupportedEncodingException e) {
                System.err.printf(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.printf(e.getMessage());
            }
        }

        ItemMetadata itemMetadata = new ItemMetadata(id, version, deviceId, parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);
        itemMetadata.setChunks(chunks);
        //itemMetadata.setTempId((long) random.nextLong());
        itemMetadata.setId((long) random.nextLong());

        return itemMetadata;
    }

    private static String doHash(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(str.getBytes("UTF-8"));

        return new BigInteger(1, crypt.digest()).toString(16);

    }
    
}
