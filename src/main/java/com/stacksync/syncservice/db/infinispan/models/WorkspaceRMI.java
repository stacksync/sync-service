package com.stacksync.syncservice.db.infinispan.models;

import com.stacksync.commons.models.ItemMetadata;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WorkspaceRMI implements Serializable {

    private static final long serialVersionUID = 243350300638953723L;

    private UUID id;
    private String name;
    private ItemRMI parentItem;
    private Integer latestRevision;
    private UUID owner;
    private String swiftContainer;
    private String swiftUrl;
    private boolean isShared;
    private boolean isEncrypted;
    private HashMap<Long, ItemRMI> items;
    private List<UUID> users;
    private long itemIdCounter, itemVersionIdCounter;

    public WorkspaceRMI() {
        this(null);
        this.itemIdCounter = 0;
        this.itemVersionIdCounter = 0;
    }

    public WorkspaceRMI(UUID id) {
        this(id, 0, null, false, false);
    }

    public WorkspaceRMI(UUID id, Integer latestRevision, UUID owner, boolean isShared, boolean isEncrypted) {
        this.id = id;
        this.latestRevision = latestRevision;
        this.owner = owner;
        this.isShared = isShared;
        this.isEncrypted = isEncrypted;
        this.items = new HashMap<Long, ItemRMI>();
        this.users = new ArrayList<UUID>();
    }

    public void setWorkspace(WorkspaceRMI workspace) {
        this.id = workspace.getId();
        this.latestRevision = workspace.getLatestRevision();
        this.owner = workspace.getOwner();
        this.isShared = workspace.isShared();
        this.isEncrypted = workspace.isEncrypted();
        this.items = workspace.getItems();
        this.users = workspace.getUsers();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getLatestRevision() {
        return latestRevision;
    }

    public void setLatestRevision(Integer latestRevision) {
        this.latestRevision = latestRevision;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean isShared() {
        return isShared;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(Boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public void setShared(Boolean isShared) {
        this.isShared = isShared;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSwiftContainer() {
        return swiftContainer;
    }

    public void setSwiftContainer(String swiftContainer) {
        this.swiftContainer = swiftContainer;
    }

    public String getSwiftUrl() {
        return swiftUrl;
    }

    public void setSwiftUrl(String swiftUrl) {
        this.swiftUrl = swiftUrl;
    }

    public ItemRMI getParentItem() {
        return parentItem;
    }

    public void setParentItem(ItemRMI parentItem) {
        this.parentItem = parentItem;
    }

    public HashMap<Long, ItemRMI> getItems() {
        return items;
    }

    public void setItems(HashMap<Long, ItemRMI> items) {
        this.items = items;
    }

    public void addItem(ItemRMI item) {
        this.items.put(item.getId(), item);
    }

    public List<UUID> getUsers() {
        return users;
    }

    public void setUsers(List<UUID> users) {
        this.users = users;
    }

    public void addUser(UUID user) {
        this.users.add(user);
    }

    public void removeUser(UUID user) {
        int index = 0;
        for (UUID id : users) {
            if (id.equals(user)) {
                users.remove(index);
                break;
            }
            index++;
        }
    }

    /**
     * Checks whether the user contains all required attributes (ID is not
     * required since it is assigned automatically when a user is inserted to
     * the database)
     *
     * @return Boolean True if the user is valid. False otherwise.
     */
    public boolean isValid() {
        return this.owner != null;
    }

    @Override
    public String toString() {
        return String.format(
                "workspace[id=%s, latestRevision=%s, owner=%s, items=%s, users=%s]", id,
                latestRevision, owner, items, users);
    }
}
