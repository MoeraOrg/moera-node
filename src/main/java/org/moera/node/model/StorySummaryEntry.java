package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryEntry {

    private String ownerName;
    private String ownerFullName;
    private String heading;

    public StorySummaryEntry() {
    }

    public StorySummaryEntry(String ownerName, String ownerFullName, String heading) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.heading = heading;
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

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

}
