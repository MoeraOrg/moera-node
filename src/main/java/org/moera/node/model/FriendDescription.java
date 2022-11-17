package org.moera.node.model;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class FriendDescription {

    @NotBlank
    @Size(max = 63)
    private String nodeName;

    private List<FriendGroupAssignment> groups;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public List<FriendGroupAssignment> getGroups() {
        return groups;
    }

    public void setGroups(List<FriendGroupAssignment> groups) {
        this.groups = groups;
    }

}
