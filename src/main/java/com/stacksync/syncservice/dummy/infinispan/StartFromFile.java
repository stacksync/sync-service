/**
 * 
 */
package com.stacksync.syncservice.dummy.infinispan;

import omq.Remote;
import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public interface StartFromFile extends Remote {
	public static String BINDING_NAME = "startFromFile";

	@AsyncMethod
	@MultiMethod
	public void startExperiment();
}
