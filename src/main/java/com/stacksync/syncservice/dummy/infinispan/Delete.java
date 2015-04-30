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
public class Delete extends Action {

    public Delete(String[] lineParts) {
        super(lineParts);
    }

    @Override
    public ItemMetadata createItemMetadata(UUID uuid, Long id, String filename) {

        super.setValues(id, null, null, false, filename, false);

        ItemMetadata itemMetadata = new ItemMetadata(id, version, uuid, parentId, parentVersion, status, modifiedAt, checksum, size,
                isFolder, filename, mimetype, chunks);

        return itemMetadata;
    }

}
