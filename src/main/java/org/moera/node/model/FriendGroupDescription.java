package org.moera.node.model;

import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.FriendGroup;

public class FriendGroupDescription {

    @NotBlank
    @Size(max = 63)
    private String title;

    private Map<String, Principal> operations;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public void toFriendGroup(FriendGroup friendGroup) {
        friendGroup.setTitle(title);
        if (getPrincipal("view") != null) {
            friendGroup.setViewPrincipal(getPrincipal("view"));
        }
    }

}
