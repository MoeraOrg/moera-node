package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Friend;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendInfo {

    private String nodeName;
    private ContactInfo contact;
    private List<FriendGroupDetails> groups;

    public FriendInfo() {
    }

    public FriendInfo(Friend friend) {
        this.nodeName = friend.getRemoteNodeName();
        this.contact = new ContactInfo(friend.getContact());
    }

    public FriendInfo(String nodeName, ContactInfo contact, List<FriendGroupDetails> groups) {
        this.nodeName = nodeName;
        this.contact = contact;
        this.groups = groups;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public ContactInfo getContact() {
        return contact;
    }

    public void setContact(ContactInfo contact) {
        this.contact = contact;
    }

    public List<FriendGroupDetails> getGroups() {
        return groups;
    }

    public void setGroups(List<FriendGroupDetails> groups) {
        this.groups = groups;
    }

}
