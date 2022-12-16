package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendOf;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupDetails implements Cloneable {

    private String id;
    private String title;
    private Long addedAt;
    private Map<String, Principal> operations;

    public FriendGroupDetails() {
    }

    public FriendGroupDetails(String id, String title, Long addedAt) {
        this.id = id;
        this.title = title;
        this.addedAt = addedAt;
    }

    public FriendGroupDetails(Friend friend, boolean isAdmin) {
        id = friend.getFriendGroup().getId().toString();
        if (isAdmin || !friend.getFriendGroup().getViewPrincipal().isAdmin()) {
            title = friend.getFriendGroup().getTitle();
        }
        addedAt = Util.toEpochSecond(friend.getCreatedAt());

        operations = new HashMap<>();
        putOperation(operations, "view", friend.getViewPrincipal(), Principal.PUBLIC);
    }

    public FriendGroupDetails(FriendOf friendOf) {
        id = friendOf.getRemoteGroupId();
        title = friendOf.getRemoteGroupTitle();
        addedAt = Util.toEpochSecond(friendOf.getRemoteAddedAt());
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public FriendGroupDetails toNonAdmin() {
        Principal viewPrincipal = getPrincipal("view", Principal.PUBLIC);
        if (viewPrincipal.isAdmin()) {
            FriendGroupDetails friendGroupDetails = clone();
            friendGroupDetails.setTitle(null);
            return friendGroupDetails;
        }
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Long addedAt) {
        this.addedAt = addedAt;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName, Principal defaultValue) {
        return operations != null ? operations.getOrDefault(operationName, defaultValue) : defaultValue;
    }

    @Override
    public FriendGroupDetails clone() {
        try {
            return (FriendGroupDetails) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Must implement Cloneable", e);
        }
    }

}
