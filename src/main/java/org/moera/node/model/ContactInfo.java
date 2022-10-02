package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Contact;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactInfo {

    private String nodeName;
    private String fullName;
    private String gender;
    private AvatarImage avatar;
    private float closeness;

    public ContactInfo() {
    }

    public ContactInfo(Contact contact) {
        nodeName = contact.getRemoteNodeName();
        fullName = contact.getRemoteFullName();
        gender = contact.getRemoteGender();
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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
