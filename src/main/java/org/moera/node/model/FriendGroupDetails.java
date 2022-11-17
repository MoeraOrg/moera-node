package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Friend;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupDetails {

    private String id;
    private String title;
    private Long addedAt;
    private Map<String, Principal> operations;

    public FriendGroupDetails() {
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

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
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

}
