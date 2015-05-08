/**
 * 
 */
package com.stacksync.syncservice.startExperimentImpl;

import omq.Remote;
import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public interface Start extends Remote {
	public static final String BINDING_NAME = "start";
	
	@AsyncMethod
	@MultiMethod
	public void startExperiment();
}