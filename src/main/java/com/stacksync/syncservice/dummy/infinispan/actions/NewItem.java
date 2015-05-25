/**
 *
 */
package com.stacksync.syncservice.dummy.infinispan.actions;

import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.postgresql.PostgresqlDAO;
import com.stacksync.syncservice.handler.Handler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class NewItem extends Action {

    private static final Logger logger = Logger.getLogger(PostgresqlDAO.class.getName());
    // timestamp,op,file_id,file_type,file_mime,file_size,file_version,sid,user_id
    // 0.003,new,1148728602,File,utext/x-python,,1,e7b48491-0a4d-4e39-9e81-f486fc404095,55a21c4e-68d7-3e27-a510-4314065ca088
    // 0.031,mod,1207353482,,,105654,2,47cae7aa-81ff-4a47-84d5-512e12c141a5,c4b359db-3bb2-31b9-ad39-231d2a94c6e2
    // 0.042,mod,3001803552,,,832,2,b1d91de2-aa05-402c-98a7-577f2eedb1c1,022df093-f6a5-3124-9dbf-efb41cc74105
    // 0.065,new,4207445328,File,uimage/jpeg,,1,e6b74dad-21a4-480a-9d3a-cc9850c4979c,76821f34-1a13-3f16-8795-5cefb8131bb8
    // 0.066,mod,688571918,,,1537,2,332e299c-513e-4ba2-a386-10c8387b1759,0654f5c9-7aca-3a41-8efd-4ada692191a1
    // 0.079,mod,1361784091,,,3672137,2,c938f5e4-8852-468d-9999-a60ac145de4b,93fbc684-a228-3a3a-901c-c1263c4def5c
    // 0.103,mod,2797693937,,,733,2,862a630d-2eae-495d-9a53-3630d2b14d2e,fa057485-0ca8-3b7a-af37-7023e237fcea

    public NewItem(Handler handler, UUID userId, Long fileId, Long fileSize, String fileType, String fileMime, Long fileVersion) {
        super(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
        status = "NEW";

        Random ran = new Random(System.currentTimeMillis());
        if (fileMime == null) {
            String[] mimes = {"pdf", "php", "java", "docx", "html", "png", "jpeg", "xml"};
            super.fileMime = mimes[ran.nextInt(mimes.length)];
        } else if (fileMime.length() > 20) {
            super.fileMime = fileMime.substring(0, 20);
        }
        if (fileSize == null) {
            int max = 8;
            int min = 1;

            super.fileSize = (long) ((ran.nextInt((max - min) + 1) + min) * CHUNK_SIZE);
        }
        if (fileType == null) {
            super.fileType = "File";
        }

    }
}
