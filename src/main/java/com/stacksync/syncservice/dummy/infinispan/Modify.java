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

    public Modify(String[] lineParts) {
        super(lineParts);
    }

    @Override
    public ItemMetadata createItemMetadata(UUID uuid, Long id, String filename) {

        super.setValues(id, 1L, null, null, false, filename, true);

        ItemMetadata itemMetadata = new ItemMetadata(super.id, super.version, uuid, super.parentId, super.parentVersion, super.status, super.modifiedAt, super.checksum, super.size,
                super.isFolder, super.filename, super.mimetype, super.chunks);
        itemMetadata.setChunks(chunks);

        return itemMetadata;
    }

}
