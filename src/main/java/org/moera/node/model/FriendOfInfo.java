package org.moera.node.model;

import org.moera.node.data.FriendOf;
import org.moera.node.util.Util;

public class FriendOfInfo {

    private String remoteNodeName;
    private ContactInfo contact;
    private FriendGroupDetails[] groups;

    public FriendOfInfo() {
    }

    public FriendOfInfo(FriendOf friendOf) {
        remoteNodeName = friendOf.getRemoteNodeName();
        if (friendOf.getContact() != null) {
            contact = new ContactInfo(friendOf.getContact());
        }
        groups = new FriendGroupDetails[] {
                new FriendGroupDetails(
                        friendOf.getRemoteGroupId(),
                        friendOf.getRemoteGroupTitle(),
                        Util.toEpochSecond(friendOf.getRemoteAddedAt())
                )
        };
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

    public FriendGroupDetails[] getGroups() {
        return groups;
    }

    public void setGroups(FriendGroupDetails[] groups) {
        this.groups = groups;
    }

}
