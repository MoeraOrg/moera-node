package org.moera.node.model;

import org.moera.node.data.Contact;

public class ContactInfo {

    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private float closeness;

    public ContactInfo() {
    }

    public ContactInfo(Contact contact) {
        nodeName = contact.getRemoteNodeName();
        fullName = contact.getRemoteFullName();
        if (contact.getRemoteAvatarMediaFile() != null) {
            avatar = new AvatarImage(contact.getRemoteAvatarMediaFile(), contact.getRemoteAvatarShape());
        }
        closeness = contact.getCloseness();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

    public float getCloseness() {
        return closeness;
    }

    public void setCloseness(float closeness) {
        this.closeness = closeness;
    }

}
