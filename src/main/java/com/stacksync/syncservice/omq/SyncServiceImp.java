package com.stacksync.syncservice.omq;

import java.util.List;
import java.util.UUID;

import omq.common.broker.Broker;
import omq.common.util.ParameterQueue;
import omq.exception.RemoteException;
import omq.server.RemoteObject;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.AccountInfo;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.notifications.CommitNotification;
import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.notifications.UpdateWorkspaceNotification;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.commons.omq.RemoteWorkspace;
import com.stacksync.commons.requests.CommitRequest;
import com.stacksync.commons.requests.GetAccountRequest;
import com.stacksync.commons.requests.GetChangesRequest;
import com.stacksync.commons.requests.GetWorkspacesRequest;
import com.stacksync.commons.requests.ShareProposalRequest;
import com.stacksync.commons.requests.UpdateDeviceRequest;
import com.stacksync.commons.requests.UpdateWorkspaceRequest;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.commons.models.SyncMetadata;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.handler.SyncHandler;
import com.stacksync.syncservice.util.Config;

public class SyncServiceImp extends RemoteObject implements ISyncService {

	private transient static final Logger logger = Logger.getLogger(SyncServiceImp.class.getName());
	private transient static final long serialVersionUID = 1L;
	private transient int index;
	private transient int numThreads;
	private transient ConnectionPool pool;
	private transient SyncHandler[] handlers;
	private transient Broker broker;

	public SyncServiceImp(Broker broker, ConnectionPool pool) throws Exception {
		super();
		this.broker = broker;
		this.pool = pool;

		index = 0;
		// Create handlers
		numThreads = Integer.parseInt(this.broker.getEnvironment().getProperty(ParameterQueue.NUM_THREADS, "1"));
		handlers = new SyncHandler[numThreads];
                
		for (int i = 0; i < numThreads; i++) {
			handlers[i] = new SQLSyncHandler(this.pool);
		}
                
	}

	@Override
	public List<SyncMetadata> getChanges(GetChangesRequest request) {
            
		logger.debug(request);
                
		User user = new User();
		user.setId(request.getUserId());
		Workspace workspace = new Workspace(request.getWorkspaceId());
                                
		List<SyncMetadata> list = getHandler().doGetChanges(user, workspace);
                
		return list;
	}

	@Override
	public List<Workspace> getWorkspaces(GetWorkspacesRequest request) throws NoWorkspacesFoundException {
		logger.debug(request.toString());

		User user = new User();
		user.setId(request.getUserId());

		List<Workspace> workspaces = getHandler().doGetWorkspaces(user);

		return workspaces;
	}

	@Override
	public void commit(CommitRequest request) {
		logger.debug(request);

		try {

			User user = new User();
			user.setId(request.getUserId());
			Device device = new Device(request.getDeviceId());
			Workspace workspace = new Workspace(request.getWorkspaceId());

			List<CommitInfo> committedItems = getHandler().doCommit(user, workspace, device, request.getItems());

			CommitNotification result = new CommitNotification(request.getRequestId(), committedItems);
			UUID id = workspace.getId();

			RemoteWorkspace commitNotifier = broker.lookupMulti(id.toString(), RemoteWorkspace.class);
			commitNotifier.notifyCommit(result);

			logger.debug("Consumer: Response sent to workspace \"" + workspace + "\"");

		} catch (Exception e) {
			logger.error(e);
		}
	}

	private synchronized SyncHandler getHandler() {
		SyncHandler handler = handlers[index++ % numThreads];
		logger.debug("Using handler: " + handler + " using connection: " + handler.getConnection());
		return handler;
	}

	@Override
	public UUID updateDevice(UpdateDeviceRequest request) throws UserNotFoundException, DeviceNotValidException,
			DeviceNotUpdatedException {

		logger.debug(request.toString());

		User user = new User();
		user.setId(request.getUserId());

		Device device = new Device();
		device.setId(request.getDeviceId());
		device.setUser(user);
		device.setName(request.getDeviceName());
		device.setOs(request.getOs());
		device.setLastIp(request.getIp());
		device.setAppVersion(request.getAppVersion());

		UUID deviceId = getHandler().doUpdateDevice(device);

		return deviceId;
	}

	@Override
	public void createShareProposal(ShareProposalRequest request) throws ShareProposalNotCreatedException,
			UserNotFoundException {

		logger.debug(request);

		User user = new User();
		user.setId(request.getUserId());
		
		Item item = new Item(request.getItemId());

		// Create share proposal
		Workspace workspace = getHandler().doShareFolder(user, request.getEmails(), item, request.isEncrypted());

		// Create notification
		ShareProposalNotification notification = new ShareProposalNotification(workspace.getId(),
				workspace.getName(), item.getId(), workspace.getOwner().getId(), workspace.getOwner().getName(),
				workspace.getSwiftContainer(), workspace.getSwiftUrl(), workspace.isEncrypted());

		notification.setRequestId(request.getRequestId());

		// Send notification to owner
		RemoteClient client;
		try {
			client = broker.lookupMulti(user.getId().toString(), RemoteClient.class);
			client.notifyShareProposal(notification);
		} catch (RemoteException e1) {
			logger.error(String.format("Could not notify user: '%s'", user.getId()), e1);
		}

		// Send notifications to users
		for (User addressee : workspace.getUsers()) {
			try {
				client = broker.lookupMulti(addressee.getId().toString(), RemoteClient.class);
				client.notifyShareProposal(notification);
			} catch (RemoteException e) {
				logger.error(String.format("Could not notify user: '%s'", addressee.getId()), e);
			}
		}
	}

	@Override
	public void updateWorkspace(UpdateWorkspaceRequest request) throws UserNotFoundException,
			WorkspaceNotUpdatedException {
		logger.debug(request);

		User user = new User();
		user.setId(request.getUserId());
		Item item = new Item(request.getParentItemId());

		Workspace workspace = new Workspace(request.getWorkspaceId());
		workspace.setName(request.getWorkspaceName());
		workspace.setParentItem(item);

		getHandler().doUpdateWorkspace(user, workspace);

		// Create notification
		UpdateWorkspaceNotification notification = new UpdateWorkspaceNotification(workspace.getId(),
				workspace.getName(), workspace.getParentItem().getId());
		notification.setRequestId(request.getRequestId());

		// Send notification to owner
		RemoteClient client;
		try {
			client = broker.lookupMulti(user.getId().toString(), RemoteClient.class);
			client.notifyUpdateWorkspace(notification);
		} catch (RemoteException e1) {
			logger.error(String.format("Could not notify user: '%s'", user.getId()), e1);
		}
	}

	@Override
	public AccountInfo getAccountInfo(GetAccountRequest request) throws UserNotFoundException {
		logger.debug(request);

		User user = getHandler().doGetUser(request.getEmail());

		AccountInfo accountInfo = new AccountInfo();

		accountInfo.setUserId(user.getId());
		accountInfo.setName(user.getName());
		accountInfo.setEmail(user.getEmail());
		accountInfo.setQuotaLimit(user.getQuotaLimit());
		accountInfo.setQuotaUsed(user.getQuotaUsed());
		accountInfo.setSwiftUser(user.getSwiftUser());
		accountInfo.setSwiftTenant(Config.getSwiftTenant());
		accountInfo.setSwiftAuthUrl(Config.getSwiftAuthUrl());

		return accountInfo;
	}
}
