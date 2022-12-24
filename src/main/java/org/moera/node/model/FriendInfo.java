package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.Contact;
import org.moera.node.data.Friend;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendInfo {

    private String nodeName;
    private ContactInfo contact;
    private List<FriendGroupDetails> groups;

    public FriendInfo() {
    }

    public FriendInfo(Contact contact, Options options, AccessChecker accessChecker) {
        this.nodeName = contact.getRemoteNodeName();
        this.contact = new ContactInfo(contact, options, accessChecker);
    }

    public FriendInfo(Friend friend, Options options, AccessChecker accessChecker) {
        this.nodeName = friend.getRemoteNodeName();
        this.contact = new ContactInfo(friend.getContact(), options, accessChecker);
    }

    public FriendInfo(String nodeName, ContactInfo contact, List<FriendGroupDetails> groups) {
        this.nodeName = nodeName;
        this.contact = contact;
        this.groups = groups;
    }

    public void protect(AccessChecker accessChecker) {
        contact.protect(accessChecker);
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
