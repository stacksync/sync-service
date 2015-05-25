/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.dummy.infinispan;

import omq.Remote;
import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;

/**
 *
 * @author cotes
 */
public interface Notifier extends Remote {
    
    public static final String BINDING_NAME = "notify";
    
    @AsyncMethod
    @MultiMethod
    public void endWarmUp();
    
    @AsyncMethod
    @MultiMethod
    public void endExperiment();
}
