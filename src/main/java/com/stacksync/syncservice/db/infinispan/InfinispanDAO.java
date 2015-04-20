/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.syncservice.db.infinispan;

import com.stacksync.commons.models.ItemMetadata;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stacksync.syncservice.db.infinispan.models.ChunkRMI;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemRMI;
import com.stacksync.syncservice.db.infinispan.models.ItemVersionRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.UserWorkspaceRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import java.util.HashMap;
import java.util.Random;

import org.infinispan.atomic.AtomicObjectFactory;

/**
 *
 * @author Laura Martínez Sanahuja <lauramartinezsanahuja@gmail.com>
 */
public class InfinispanDAO implements InfinispanWorkspaceDAO, InfinispanItemDAO, InfinispanItemVersionDAO, InfinispanUserDAO, InfinispanDeviceDAO, Serializable {

    private WorkspaceRMI workspace;
    private UserRMI user;
    private HashMap<String, UUID> mailUser;
    private final AtomicObjectFactory factory;
    private Random random;

    public InfinispanDAO(AtomicObjectFactory factory) {

        this.random = new Random();
        this.factory = factory;
        mailUser = (HashMap<String, UUID>) factory.getInstanceOf(HashMap.class, "mailUser", false, null, false);

    }

    //************************************
    //************************************
    //************* WORKSPACE ************
    //************************************
    //************************************
    @Override
    public WorkspaceRMI getById(UUID id) throws RemoteException {

        workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, id.toString(), false, null, false);
        return workspace;

    }

    @Override
    public List<WorkspaceRMI> getByUserId(UUID userId) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, userId.toString(), false, null, false);
        List<UUID> list = user.getWorkspaces();

        List<WorkspaceRMI> result = new ArrayList<WorkspaceRMI>();

        for (UUID id : list) {
            workspace = getById(id);
            if (workspace.getId() != null) {
                result.add(workspace);
            }
        }

        return result;

    }

    @Override
    public WorkspaceRMI getDefaultWorkspaceByUserId(UUID userId) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, userId.toString(), false, null, false);
        List<UUID> workspaces = user.getWorkspaces();

        WorkspaceRMI defaultWorkspace = null;
        for (UUID w : workspaces) {
            WorkspaceRMI wspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, w.toString(), false, null, false);
            if (!wspace.isShared()) {
                defaultWorkspace = wspace;
                break;
            }
        }
        return defaultWorkspace;

    }

    @Override
    public WorkspaceRMI getByItemId(Long itemId) throws RemoteException {
        //Senseless

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public void add(WorkspaceRMI wspace) throws RemoteException {
        //Maybe it should be named create

        workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, wspace.getId().toString(), false, null, false);
        workspace.setWorkspace(wspace);
        //factory.disposeInstanceOf(WorkspaceRMI.class, wspace.getId().toString(), true);

    }

    @Override
    public void update(UserRMI usr, WorkspaceRMI wspace) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, usr.getId().toString(), false, null, false);

        List<UUID> list = user.getWorkspaces();

        /*for (UUID w : list) {
         if (w.equals(wspace.getId())) {
         factory.disposeInstanceOf(WorkspaceRMI.class, w.toString(), true);
         }
         }*/
    }

    @Override
    public void deleteWorkspace(UUID id) throws RemoteException {

        workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, id.toString(), false, null, false);

        List<UUID> list = workspace.getUsers();

        for (UUID u : list) {
            user = (UserRMI) factory.getInstanceOf(UserRMI.class, u.toString(), false, null, false);
            user.removeWorkspace(id);
            //factory.disposeInstanceOf(UserRMI.class, u.toString(), true);
        }

        //factory.disposeInstanceOf(WorkspaceRMI.class, id.toString(), false);
    }

    @Override
    public void addUser(UserRMI usr, WorkspaceRMI wspace) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, usr.getId().toString(), false, null, false);
        workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, wspace.getId().toString(), false, null, false);

        user.addWorkspace(workspace.getId());
        workspace.addUser(user.getId());

        //factory.disposeInstanceOf(UserRMI.class, usr.getId().toString(), true);
        //factory.disposeInstanceOf(UserRMI.class, wspace.getId().toString(), true);
    }

    @Override
    public void deleteUser(UserRMI usr, WorkspaceRMI wspace) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, usr.getId().toString(), false, null, false);
        workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, wspace.getId().toString(), false, null, false);

        workspace.removeUser(usr.getId());
        user.removeWorkspace(wspace.getId());

        //factory.disposeInstanceOf(UserRMI.class, usr.getId().toString(), true);
        //factory.disposeInstanceOf(UserRMI.class, wspace.getId().toString(), true);
    }

    @Override
    public List<UserWorkspaceRMI> getMembersById(UUID wspaceId) throws RemoteException {

        workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, wspaceId.toString(), false, null, false);

        List<UUID> usrs = workspace.getUsers();
        UUID owner = workspace.getOwner();
        if (!usrs.contains(owner)) {
            usrs.add(owner);
            workspace.addUser(owner);
        }
        List<UserWorkspaceRMI> result = new ArrayList<UserWorkspaceRMI>();
        UserWorkspaceRMI usrwspace;

        for (UUID uuid : usrs) {
            user = (UserRMI) factory.getInstanceOf(UserRMI.class, uuid.toString(), false, null, false);
            usrwspace = new UserWorkspaceRMI(user, workspace);
            result.add(usrwspace);
        }

        return result;

    }

    //************************************
    //************************************
    //*************** ITEM ***************
    //************************************
    //************************************
    @Override
    public ItemRMI findById(Long id) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        return items.get(id);
    }

    @Override
    public void add(ItemRMI item) throws RemoteException {

        workspace.addItem(item);

    }

    @Override
    public void update(ItemRMI item) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        items.put(item.getId(), item);

        //factory.disposeInstanceOf(WorkspaceRMI.class, workspace.getId().toString(), true);
    }

    @Override
    public void put(ItemRMI item) throws RemoteException {

        if (item.getId() == null) {
            item.setId(this.random.nextLong());
            add(item);
        } else {
            update(item);
        }

    }

    @Override
    public void delete(Long id) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        items.remove(id);
    }

    private ItemMetadata getItemMetadataFromItem(ItemRMI item) {

        return getItemMetadataFromItem(item, item.getLatestVersionNumber(), false, false, false);

    }

    private ItemMetadata getItemMetadataFromItem(ItemRMI item, Long version, Boolean includeList, Boolean includeDeleted, Boolean includeChunks) {

        ItemMetadata itemMetadata = null;

        List<ItemVersionRMI> versions = item.getVersions();
        for (ItemVersionRMI itemVersion : versions) {
            if (itemVersion.getVersion().equals(version)) {
                itemMetadata = createItemMetadataFromItemAndItemVersion(item, itemVersion, includeChunks);
                if (includeList && item.isFolder()) {
                    // Get children :D
                    itemMetadata = addChildrenFromItemMetadata(itemMetadata, includeDeleted);
                }
                break;
            }
        }

        return itemMetadata;

    }

    private ItemMetadata addChildrenFromItemMetadata(ItemMetadata itemMetadata, Boolean includeDeleted) {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        ItemRMI item = items.get(itemMetadata.getId());

        if (item != null && ((includeDeleted && itemMetadata.getStatus().equals("DELETED")) || !itemMetadata.getStatus().equals("DELETED"))) {
            ItemVersionRMI thisItemVersion = item.getLatestVersion();
            ItemMetadata child = createItemMetadataFromItemAndItemVersion(item, thisItemVersion);
            itemMetadata.addChild(child);
        }

        return itemMetadata;

    }

    private ItemMetadata createItemMetadataFromItemAndItemVersion(ItemRMI item, ItemVersionRMI itemVersion) {

        return createItemMetadataFromItemAndItemVersion(item, itemVersion, false);

    }

    private ItemMetadata createItemMetadataFromItemAndItemVersion(ItemRMI item, ItemVersionRMI itemVersion, Boolean includeChunks) {

        ArrayList<String> chunks = new ArrayList<String>();
        if (includeChunks) {
            for (ChunkRMI chunk : itemVersion.getChunks()) {
                chunks.add(chunk.toString());
            }
        } else {
            chunks = null;
        }

        return new ItemMetadata(item.getId(), itemVersion.getVersion(), itemVersion.getDevice().getId(), item.getParentId(), item.getClientParentFileVersion(), itemVersion.getStatus(), itemVersion.getModifiedAt(), itemVersion.getChecksum(), itemVersion.getSize(), item.isFolder(), item.getFilename(), item.getMimetype(), chunks);

    }

    @Override
    public List<ItemMetadata> getItemsByWorkspaceId(UUID workspaceId) throws RemoteException {

        List<ItemMetadata> result = new ArrayList<ItemMetadata>();
        ItemMetadata itemMetadata;

        workspace = getById(workspaceId);

        HashMap<Long, ItemRMI> items = workspace.getItems();
        for (Long id : items.keySet()) {
            ItemRMI item = items.get(id);
            itemMetadata = getItemMetadataFromItem(item);
            if (itemMetadata != null) {
                result.add(itemMetadata);
            }
        }

        return result;

    }

    @Override
    public List<ItemMetadata> getItemsById(Long id) throws RemoteException {

        List<ItemMetadata> result = new ArrayList<ItemMetadata>();
        // TODO FIX ME
        /*ItemMetadata itemMetadata;

         List<ItemRMI> list = workspace.getItems();

         for (ItemRMI item : list) {
         if (item.getId().equals(id) || (item.getParentId() != null && item.getParentId().equals(id))) {
         itemMetadata = getItemMetadataFromItem(item);
         if (itemMetadata != null) {
         result.add(itemMetadata);
         }
         }
         }*/

        return result;

    }

    @Override
    public ItemMetadata findById(Long id, Boolean includeList, Long version, Boolean includeDeleted, Boolean includeChunks) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        ItemRMI item = items.get(id);
        ItemMetadata itemMetadata = null;
        if (item != null) {
            itemMetadata = getItemMetadataFromItem(item, version, includeList, includeDeleted, includeChunks);
        }

        return itemMetadata;

    }

    @Override
    public ItemMetadata findByUserId(UUID serverUserId, Boolean includeDeleted) throws RemoteException {
        //Senseless

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public ItemMetadata findItemVersionsById(Long id) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        ItemRMI item = items.get(id);
        ItemMetadata itemMetadata = null;

        if (item == null) {
            return null;
        }

        for (ItemVersionRMI itemVersion : item.getVersions()) {
            if (itemVersion.getVersion().equals(item.getLatestVersionNumber())) {
                itemMetadata = createItemMetadataFromItemAndItemVersion(item, itemVersion);
                break;
            }
        }

        if (itemMetadata == null) {
            return null;
        }

        for (ItemVersionRMI itemVersion : item.getVersions()) {
            if (!itemVersion.getVersion().equals(item.getLatestVersionNumber())) {
                ItemMetadata version = createItemMetadataFromItemAndItemVersion(item, itemVersion);
                if (version != null) {
                    itemMetadata.addChild(version);
                }
            }
        }

        return itemMetadata;

    }

    @Override
    public List<String> migrateItem(Long itemId, UUID workspaceId) throws RemoteException {
        //Senseless

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    //************************************
    //************************************
    //************ ITEMVERSION ***********
    //************************************
    //************************************
    @Override
    public ItemMetadata findByItemIdAndVersion(Long id, Long version) throws RemoteException {

        return findById(id, Boolean.FALSE, version, Boolean.FALSE, Boolean.FALSE);

    }

    @Override
    public void add(ItemVersionRMI itemVersion) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        ItemRMI item = items.get(itemVersion.getItemId());
        if (item != null) {
            itemVersion.setId(this.random.nextLong());
            item.addVersion(itemVersion);
            item.setLatestVersionNumber(itemVersion.getVersion());
        }

    }

    @Override
    public void insertChunk(Long itemVersionId, Long chunkId, Integer order) throws RemoteException {
        //Senseless

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public void insertChunks(long id, List<ChunkRMI> chunks, long itemVersionId) throws RemoteException {

        HashMap<Long, ItemRMI> items = workspace.getItems();
        ItemRMI item = items.get(id);
        List<ItemVersionRMI> versions;

        versions = item.getVersions();
        for (ItemVersionRMI version : versions) {
            if (version.getId().equals(itemVersionId)) {
                version.setChunks(chunks);
            }
        }

    }

    @Override
    public List<ChunkRMI> findChunks(Long itemVersionId) throws RemoteException {

        // TODO FIX ME
        /*List<ItemRMI> items = workspace.getItems();
        List<ItemVersionRMI> versions;

        for (ItemRMI item : items) {
            versions = item.getVersions();
            for (ItemVersionRMI version : versions) {
                if (version.getId().equals(itemVersionId)) {
                    return version.getChunks();
                }
            }
        }
        */
        return null;

    }

    @Override
    public void update(ItemVersionRMI itemVersion) throws RemoteException {

        /*List<ItemRMI> items = workspace.getItems();

        for (ItemRMI item : items) {
            if (item.getId().equals(itemVersion.getItemId())) {
                List<ItemVersionRMI> versions = item.getVersions();
                for (ItemVersionRMI version : versions) {
                    if (version.getVersion().equals(itemVersion.getVersion())) {
                        item.removeVersion(version);
                        item.addVersion(itemVersion);
                        break;
                    }
                }
                break;
            }
        }*/

    }

    @Override
    public void delete(ItemVersionRMI itemVersion) throws RemoteException {

        /*List<ItemRMI> items = workspace.getItems();

        for (ItemRMI item : items) {
            if (item.getId().equals(itemVersion.getItemId())) {
                item.removeVersion(itemVersion);
                if (item.getLatestVersionNumber().equals(itemVersion.getVersion())) {
                    item.setLatestVersionNumber(itemVersion.getVersion() - 1L);
                }
                break;
            }
        }*/

    }

    //************************************
    //************************************
    //*************** USER ***************
    //************************************
    //************************************
    @Override
    public UserRMI findById(UUID id) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, id.toString(), false, null, false);
        return user;

    }

    @Override
    public UserRMI getByEmail(String email) throws RemoteException {
        UUID userId = mailUser.get(email);
        return this.findById(userId);
    }

    @Override
    public List<UserRMI> findAll() throws RemoteException {
        //Senseless

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public List<UserRMI> findByItemId(Long clientFileId) throws RemoteException {
        //Senseless

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public void add(UserRMI usr) throws RemoteException {
        //Maybe it should be named create

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, usr.getId().toString(), false, null, false);
        user.setUser(usr);

        //factory.disposeInstanceOf(UserRMI.class, usr.getId().toString(), true);
        mailUser.put(usr.getEmail(), usr.getId());
        //factory.disposeInstanceOf(HashMap.class, "mailUser", true);
    }

    @Override
    public void update(UserRMI usr) throws RemoteException {
        //add and update are the same

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, usr.getId().toString(), false, null, false);
        user.setUser(usr);
        //factory.disposeInstanceOf(UserRMI.class, usr.getId().toString(), true);

    }

    @Override
    public void deleteUser(UUID id) throws RemoteException {

        user = (UserRMI) factory.getInstanceOf(UserRMI.class, id.toString(), false, null, false);

        List<UUID> list = user.getWorkspaces();

        for (UUID w : list) {
            workspace = (WorkspaceRMI) factory.getInstanceOf(WorkspaceRMI.class, w.toString(), false, null, false);
            workspace.removeUser(id);
            //factory.disposeInstanceOf(WorkspaceRMI.class, w.toString(), true);
        }

        //factory.disposeInstanceOf(UserRMI.class, id.toString(), false);
    }

    //************************************
    //************************************
    //************** DEVICE **************
    //************************************
    //************************************
    @Override
    public DeviceRMI get(UUID id) throws RemoteException {

        List<DeviceRMI> devices = user.getDevices();

        for (DeviceRMI device : devices) {
            if (device.getId().equals(id)) {
                return device;
            }
        }

        return null;

    }

    @Override
    public void add(DeviceRMI device) throws RemoteException {

        List<DeviceRMI> devices = user.getDevices();

        if (!devices.contains(device)) {
            user.addDevice(device);
        }

        //factory.disposeInstanceOf(UserRMI.class, user.getId().toString(), true);
    }

    @Override
    public void update(DeviceRMI device) throws RemoteException {

        List<DeviceRMI> devices = user.getDevices();

        for (DeviceRMI currentDevice : devices) {
            if (currentDevice.getId().equals(device.getId())) {
                devices.remove(currentDevice);
                devices.add(device);
                break;
            }
        }

        user.setDevices(devices);

        //factory.disposeInstanceOf(UserRMI.class, user.getId().toString(), true);
    }

    @Override
    public void deleteDevice(UUID id) throws RemoteException {

        List<DeviceRMI> devices = user.getDevices();

        for (DeviceRMI currentDevice : devices) {
            if (currentDevice.getId().equals(id)) {
                devices.remove(currentDevice);
                break;
            }
        }

        user.setDevices(devices);

        //factory.disposeInstanceOf(UserRMI.class, user.getId().toString(), true);
    }
}
