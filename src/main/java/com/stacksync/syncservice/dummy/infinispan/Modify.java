/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import static com.stacksync.syncservice.dummy.infinispan.AServerDummy.CHUNK_SIZE;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class Modify extends Action {

    public Modify(String[] lineParts) {
        super(lineParts);
    }

    @Override
    public ItemMetadata createItemMetadata(Random ran, int min, int max, UUID uuid, Long id, String filename) {

        List<String> chunks = new ArrayList<String>();
        // Fill chunks
        int numChunks = ran.nextInt((max - min) + 1) + min;
        long size = numChunks * CHUNK_SIZE;
        for (int i = 0; i < numChunks; i++) {
            String str = java.util.UUID.randomUUID().toString();
            try {
                chunks.add(super.doHash(str));
            } catch (UnsupportedEncodingException e) {
                System.err.println(e.toString());
            } catch (NoSuchAlgorithmException e) {
                System.err.println(e.toString());
            }
        }

        super.setValues(ran, id, 1L, null, null, (long) ran.nextInt(Integer.MAX_VALUE), new ArrayList<String>(), false, filename, numChunks, size);

        ItemMetadata itemMetadata = new ItemMetadata(super.id, super.version, uuid, super.parentId, super.parentVersion, super.status, super.modifiedAt, super.checksum, super.size,
                super.isFolder, super.filename, super.mimetype, super.chunks);
        itemMetadata.setChunks(chunks);

        return itemMetadata;
    }

}
