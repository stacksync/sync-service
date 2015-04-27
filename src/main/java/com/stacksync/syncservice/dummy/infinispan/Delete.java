/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import java.util.ArrayList;
import java.util.Random;
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
    public ItemMetadata createItemMetadata(Random ran, int min, int max, UUID uuid, Long id, String filename) {
        super.setValues(ran, id, 1L, null, null, (long) ran.nextInt(Integer.MAX_VALUE), new ArrayList<String>(), false, filename, numChunks, size);

        ItemMetadata itemMetadata = new ItemMetadata(super.id, super.version, uuid, super.parentId, super.parentVersion, super.status, super.modifiedAt, super.checksum, super.size,
                super.isFolder, super.filename, super.mimetype, super.chunks);

        return itemMetadata;
    }

}
