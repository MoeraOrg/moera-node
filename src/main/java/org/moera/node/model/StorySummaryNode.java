package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryNode {

    private String ownerName;
    private String ownerFullName;

    public StorySummaryNode() {
    }

    public StorySummaryNode(String ownerName, String ownerFullName) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
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

}
