package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendInfo {

    private String nodeName;
    private List<FriendGroupDetails> groups;

    public FriendInfo() {
    }

    public FriendInfo(String nodeName, List<FriendGroupDetails> groups) {
        this.nodeName = nodeName;
        this.groups = groups;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public List<FriendGroupDetails> getGroups() {
        return groups;
    }

    public void setGroups(List<FriendGroupDetails> groups) {
        this.groups = groups;
    }

}
