package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.FriendGroup;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupInfo {

    private String id;
    private String title;
    private Long createdAt;
    private Map<String, Principal> operations;

    public FriendGroupInfo() {
    }

    public FriendGroupInfo(FriendGroup friendGroup) {
        id = friendGroup.getId().toString();
        title = friendGroup.getTitle();
        createdAt = Util.toEpochSecond(friendGroup.getCreatedAt());

        operations = new HashMap<>();
        putOperation(operations, "view", friendGroup.getViewPrincipal(), Principal.PUBLIC);
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
