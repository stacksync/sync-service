/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.UserWorkspaceRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMIShardered;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface InfinispanWorkspaceDAOShardered extends Remote {

    public WorkspaceRMIShardered getById(UUID id) throws RemoteException;

    public List<WorkspaceRMIShardered> getByUserId(UUID userId) throws RemoteException;

    public WorkspaceRMIShardered getDefaultWorkspaceByUserId(UUID userId) throws RemoteException;

    public WorkspaceRMIShardered getByItemId(Long itemId) throws RemoteException;

    public void add(WorkspaceRMIShardered workspace) throws RemoteException;

    public void update(UserRMI user, WorkspaceRMIShardered workspace) throws RemoteException;

    public void addUser(UserRMI user, WorkspaceRMIShardered workspace) throws RemoteException;

    public void deleteUser(UserRMI user, WorkspaceRMIShardered workspace) throws RemoteException;

    public void deleteWorkspace(UUID id) throws RemoteException;

    public List<UserWorkspaceRMI> getMembersById(UUID workspaceId) throws RemoteException;

}
