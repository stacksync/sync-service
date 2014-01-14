package com.stacksync.syncservice.omq;

import java.util.ArrayList;
import java.util.List;

import omq.common.broker.Broker;
import omq.common.util.ParameterQueue;
import omq.server.RemoteObject;

import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.exceptions.DAOException;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.model.Device;
import com.stacksync.syncservice.model.User;
import com.stacksync.syncservice.model.Workspace;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.models.DeviceInfo;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.models.WorkspaceInfo;
import com.stacksync.syncservice.omq.ISyncService;
import com.stacksync.syncservice.rpc.messages.Commit;
import com.stacksync.syncservice.rpc.messages.GetWorkspaces;
import com.stacksync.syncservice.rpc.messages.GetWorkspacesResponse;

public class SyncServiceImp extends RemoteObject implements ISyncService {

	private transient static final Logger logger = Logger.getLogger(SyncServiceImp.class.getName());
	private transient static final long serialVersionUID = 1L;
	private transient int index;
	private transient int numThreads;
	private transient ConnectionPool pool;
	private transient Handler[] handlers;
	private transient Broker broker;

	public SyncServiceImp(Broker broker, ConnectionPool pool) throws Exception {
		super();
		this.broker = broker;
		this.pool = pool;

		index = 0;
		// Create handlers
		numThreads = Integer.parseInt(this.broker.getEnvironment().getProperty(ParameterQueue.NUM_THREADS, "1"));
		handlers = new Handler[numThreads];

		for (int i = 0; i < numThreads; i++) {
			handlers[i] = new SQLHandler(this.pool);
		}

	}

	@Override
	public List<ObjectMetadata> getChanges(String user, String requestId, WorkspaceInfo workspace) {
		logger.debug("GetChanges -->[User:" + user + ", Request:" + requestId + ", Workspace: " + workspace + "]");

		List<ObjectMetadata> list = getHandler().doGetChanges(workspace.getIdentifier(), user);
		return list;
	}

	@Override
	public List<WorkspaceInfo> getWorkspaces(String user, String requestId) {
		logger.debug("GetWorkspaces -->[User:" + user + ", Request:" + requestId + "]");
		GetWorkspaces workspacesRequest = new GetWorkspaces(requestId, user, "");
		GetWorkspacesResponse response = getHandler().doGetWorkspaces(workspacesRequest);

		List<WorkspaceInfo> list = new ArrayList<WorkspaceInfo>();
		for (Workspace w : response.getWorkspaces()) {
			try {
				WorkspaceInfo workspace = new WorkspaceInfo(w.getClientWorkspaceName(), w.getLatestRevision(), "/");
				list.add(workspace);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return list;
	}

	@Override
	public void commit(String user, String requestId, WorkspaceInfo workspace, String device, List<ObjectMetadata> commitObjects) {
		logger.debug("Commit -->[User:" + user + ", Request:" + requestId + ", RemoteWorkspace:" + workspace + ", Device: " + device + "]");
		logger.debug("Commit objects -> " + commitObjects);
		Commit commitRequest = new Commit(user, requestId, commitObjects, device, workspace.getIdentifier());

		try {
			CommitResult result = getHandler().doCommit(commitRequest);

			RemoteWorkspace commitNotifier = broker.lookupMulti(workspace.getIdentifier(), RemoteWorkspace.class);

			commitNotifier.notifyCommit(result);

			logger.debug("Consumer: Response sent to workspace \"" + workspace + "\"");
		} catch (DAOException e) {
			// debe enterarse el cliente?
			// se recupera?
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private synchronized Handler getHandler() {
		Handler handler = handlers[index++ % numThreads];
		logger.debug("Using handler: " + handler + " using connection: " + handler.getConnection());
		return handler;
	}

	@Override
	public Long updateDevice(String user, String requestId, DeviceInfo deviceInfo) {
		
		User user1 = new User();
		user1.setCloudId(user);
		
		Device device = new Device();
		device.setId(deviceInfo.getId());
		device.setUser(user1);
		device.setName(deviceInfo.getName());
		device.setOs(deviceInfo.getOs());
		device.setLastIp(deviceInfo.getIp());
		device.setAppVersion(deviceInfo.getAppVersion());
		
		logger.debug(String.format("RequestId=%s, updateDevice: %s", requestId, device.toString()));
		
		Long deviceId = getHandler().doUpdateDevice(device);
		
		return deviceId;
	}

}
