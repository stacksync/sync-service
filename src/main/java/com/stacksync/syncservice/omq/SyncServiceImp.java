package com.stacksync.syncservice.omq;

import com.stacksync.commons.exceptions.*;
import com.stacksync.commons.models.*;
import com.stacksync.commons.notifications.UpdateWorkspaceNotification;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.commons.requests.*;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.infinispan.models.*;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.SQLSyncHandler;
import com.stacksync.syncservice.handler.SyncHandler;
import com.stacksync.syncservice.util.Config;
import omq.common.broker.Broker;
import omq.common.util.ParameterQueue;
import omq.exception.RemoteException;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public List<ItemMetadata> getChanges(GetChangesRequest request) {

        logger.debug(request);

        UserRMI user = new UserRMI();
        user.setId(request.getUserId());
        WorkspaceRMI workspace = new WorkspaceRMI(request.getWorkspaceId());

        List<ItemMetadata> result = new ArrayList<>();
        for (ItemMetadataRMI metadataRMI : getHandler().doGetChanges(user, workspace)) {
            result.add(metadataRMI.toMetadataItem());
        }
        return result;
    }

    @Override
    public List<Workspace> getWorkspaces(GetWorkspacesRequest request) throws NoWorkspacesFoundException {
        logger.debug(request.toString());

        UserRMI user = new UserRMI();
        user.setId(request.getUserId());

        List<WorkspaceRMI> workspaces = getHandler().doGetWorkspaces(user);

        List<Workspace> w = convertWorkspaces(workspaces);

        return w;
    }

    @Override
    public void commit(CommitRequest request) {
        logger.debug(request);

        try {
            
            long startTime = System.currentTimeMillis();
            
            UserRMI user = new UserRMI();
            user.setId(request.getUserId());
            List<ItemMetadataRMI> itemMetadataRMIList = new ArrayList<>();
            for (ItemMetadata metadata : request.getItems()) {
                itemMetadataRMIList.add(new ItemMetadataRMI(metadata));
            }
            List<CommitInfo> committedItems =
                  getHandler().doCommit(request.getUserId(), request.getWorkspaceId(), request.getDeviceId(), itemMetadataRMIList);
            long endTime = System.currentTimeMillis();
            logger.info("\tRequestId= " + request.getRequestId() + " - TotalTime: " + (endTime - startTime));
            
            /*CommitNotification result = new CommitNotification(request.getRequestId(), committedItems);
             UUID id = workspace.getId();

             RemoteWorkspace commitNotifier = broker.lookupMulti(id.toString(), RemoteWorkspace.class);
             commitNotifier.notifyCommit(result);

             logger.debug("Consumer: Response sent to workspace \"" + workspace + "\"");
            logger.info("End:" + request.getRequestId());*/

        } catch (DAOException e) {
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

        UserRMI user = new UserRMI();
        user.setId(request.getUserId());

        DeviceRMI device = new DeviceRMI(request.getDeviceId(), request.getDeviceName(), user);
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

        /*UserRMI user = new UserRMI();
         user.setId(request.getUserId());
		
         ItemRMI item = new ItemRMI(request.getItemId());

         // Create share proposal
         WorkspaceRMI workspace = getHandler().doShareFolder(user, request.getEmails(), item, request.isEncrypted());

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
         for (UserRMI addressee : workspace.getUsers()) {
         try {
         client = broker.lookupMulti(addressee.getId().toString(), RemoteClient.class);
         client.notifyShareProposal(notification);
         } catch (RemoteException e) {
         logger.error(String.format("Could not notify user: '%s'", addressee.getId()), e);
         }
         }*/
    }

    @Override
    public void updateWorkspace(UpdateWorkspaceRequest request) throws UserNotFoundException,
            WorkspaceNotUpdatedException {
        logger.debug(request);

        UserRMI user = new UserRMI(request.getUserId());
        ItemRMI item = new ItemRMI(request.getParentItemId());

        WorkspaceRMI workspace = new WorkspaceRMI(request.getWorkspaceId());
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

        UserRMI user = getHandler().doGetUser(request.getEmail());

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

    private List<Workspace> convertWorkspaces(List<WorkspaceRMI> workspaces) {
        Workspace wspace;
        User user;
        List<Workspace> wspaces = new ArrayList<>();

        for (WorkspaceRMI workspace : workspaces) {
            user = new User(workspace.getOwner().getId());
            wspace = new Workspace(workspace.getId(), workspace.getLatestRevision(), user, workspace.isShared(), workspace.isEncrypted());
            wspaces.add(wspace);
        }

        return wspaces;
    }

    @Override
    public void createUser(UUID userId) {
        try {
            getHandler().createUser(userId);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(String.format("Could not create user: '%s'", userId), ex);
        }
    }
}
