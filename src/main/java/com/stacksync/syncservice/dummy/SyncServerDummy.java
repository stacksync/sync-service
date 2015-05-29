/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy;

import com.stacksync.syncservice.SyncServiceDaemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;

/**
 *
 * @author sergi
 */
public class SyncServerDummy {

    public static void main(String[] args) throws Exception {

        SyncServiceDaemon daemon = new SyncServiceDaemon();
        try {
            DaemonContext dc = new DaemonContext() {

                @Override
                public DaemonController getController() {
                    return null;
                }

                @Override
                public String[] getArguments() {
                    return new String[]{"config.properties"};
                }
            };

            daemon.init(dc);
            daemon.start();
        } catch (Exception e) {
            throw e;
        }
    }
}
