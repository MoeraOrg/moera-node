package org.moera.node.model;

import java.util.Map;
import java.util.UUID;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Friend;

public class FriendGroupAssignment {

    private UUID id;

    private Map<String, Principal> operations;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public void toFriend(Friend friend) {
        if (getPrincipal("view") != null) {
            friend.setViewPrincipal(getPrincipal("view"));
        }
    }

}
