/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy;

import omq.Remote;
import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;

/**
 *
 * @author cotes
 */
public interface Start extends Remote {

    public static final String BINDING_NAME = "start";

    @AsyncMethod
    @MultiMethod
    public void startWarmUp(int numThreads, int numUsers, int commitsPerSecond, int minutes);

    @AsyncMethod
    @MultiMethod
    public void startExperiment();
}
