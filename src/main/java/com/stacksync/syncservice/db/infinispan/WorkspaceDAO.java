/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public interface WorkspaceDAO extends Remote {

    WorkspaceRMI getById(UUID id) throws RemoteException;

    List<WorkspaceRMI> getByUserId(UUID userId) throws RemoteException;

    WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId) throws RemoteException;

    WorkspaceRMI getByItemId(Long itemId) throws RemoteException;

    void add(WorkspaceRMI workspace) throws RemoteException;

    void update(UserRMI user, WorkspaceRMI workspace) throws RemoteException;

    void addUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException;

    void deleteUser(UserRMI user, WorkspaceRMI workspace) throws RemoteException;

    void deleteWorkspace(UUID id) throws RemoteException;

    List<UserRMI> getMembersById(UUID workspaceId) throws RemoteException;

}
