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
public class New extends Action {

    public New(String[] lineParts) {
        super(lineParts);
    }

    @Override
    public ItemMetadata createItemMetadata(UUID uuid, Long id, String filename) {

        setValues(null, 1L, null, null, false, filename, true);

        ItemMetadata itemMetadata = new ItemMetadata(this.id, version, uuid, parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);
        itemMetadata.setChunks(chunks);
        itemMetadata.setTempId(super.getTempId());

        return itemMetadata;
    }

}
