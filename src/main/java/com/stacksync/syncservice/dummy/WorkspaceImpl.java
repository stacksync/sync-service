package com.stacksync.syncservice.dummy;

import org.apache.log4j.Logger;

import omq.client.annotation.AsyncMethod;
import omq.client.annotation.MultiMethod;
import omq.server.RemoteObject;

import com.stacksync.commons.notifications.CommitNotification;
import com.stacksync.commons.omq.RemoteWorkspace;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */

public class WorkspaceImpl extends RemoteObject implements RemoteWorkspace {
	private final Logger logger = Logger.getLogger(WorkspaceImpl.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	@MultiMethod
	@AsyncMethod
	public void notifyCommit(CommitNotification commitNotification) {
		logger.info("RequestID="+commitNotification.getRequestId());
	}

}
