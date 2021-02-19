package org.moera.node.model;

import org.moera.node.data.Contact;

public class ContactInfo {

    private String nodeName;
    private String fullName;
    private float closeness;

    public ContactInfo() {
    }

    public ContactInfo(Contact contact) {
        nodeName = contact.getRemoteNodeName();
        fullName = contact.getRemoteFullName();
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

    public float getCloseness() {
        return closeness;
    }

    public void setCloseness(float closeness) {
        this.closeness = closeness;
    }

}
