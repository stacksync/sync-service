package com.stacksync.syncservice.test.handler;

import java.util.UUID;

import omq.common.broker.Broker;

import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.syncservice.util.Config;

public class SendShareNotificationTest {

	public static void main(String[] args) throws Exception {

		Config.loadProperties();
		Broker broker = new Broker(Config.getProperties());

		ShareProposalNotification notification = new ShareProposalNotification(UUID.randomUUID(), "folder name", null,
				UUID.randomUUID(), "Owner name", "container999", "http://asdasddsa", false, false);

		RemoteClient client = broker.lookupMulti("AUTH_b9d5665bc46145b7985c2e0c37f817d1", RemoteClient.class);
		client.notifyShareProposal(notification);
	}
}
