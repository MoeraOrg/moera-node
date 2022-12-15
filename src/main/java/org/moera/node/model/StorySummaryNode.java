package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Contact;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryNode {

    private String ownerName;
    private String ownerFullName;
    private String ownerGender;

    public StorySummaryNode() {
    }

    public StorySummaryNode(String ownerName, String ownerFullName, String ownerGender) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
    }

    public StorySummaryNode(Contact contact) {
        this(contact.getRemoteNodeName(), contact.getRemoteFullName(), contact.getRemoteGender());
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
    }

}
