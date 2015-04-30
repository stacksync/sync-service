/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class Modify extends Action {
    
    private long version;

    public Modify(String[] lineParts) {
        super(lineParts);
    }

    @Override
    public ItemMetadata createItemMetadata(UUID uuid, Long id, String filename) {

        super.setValues(id, null, null, false, filename, true);

        ItemMetadata itemMetadata = new ItemMetadata(id, version, uuid, parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);
        itemMetadata.setChunks(chunks);

        return itemMetadata;
    }

}
