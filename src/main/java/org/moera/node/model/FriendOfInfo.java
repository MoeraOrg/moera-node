package org.moera.node.model;

import java.util.Collections;
import java.util.List;

import org.moera.node.data.FriendOf;
import org.moera.node.util.Util;

public class FriendOfInfo {

    private String remoteNodeName;
    private ContactInfo contact;
    private List<FriendGroupDetails> groups;

    public FriendOfInfo() {
    }

    public FriendOfInfo(FriendOf friendOf) {
        remoteNodeName = friendOf.getRemoteNodeName();
        if (friendOf.getContact() != null) {
            contact = new ContactInfo(friendOf.getContact());
        }
        groups = Collections.singletonList(
                new FriendGroupDetails(
                        friendOf.getRemoteGroupId(),
                        friendOf.getRemoteGroupTitle(),
                        Util.toEpochSecond(friendOf.getRemoteAddedAt())
                )
        );
    }

    public FriendOfInfo(String remoteNodeName, ContactInfo contact, List<FriendGroupDetails> groups) {
        this.remoteNodeName = remoteNodeName;
        this.contact = contact;
        this.groups = groups;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
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
