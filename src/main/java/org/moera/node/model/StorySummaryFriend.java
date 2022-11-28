package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryFriend {

    private String ownerName;
    private String ownerFullName;
    private String ownerGender;
    private String friendGroupTitle;

    public StorySummaryFriend() {
    }

    public StorySummaryFriend(String ownerName, String ownerFullName, String ownerGender, String friendGroupTitle) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        this.friendGroupTitle = friendGroupTitle;
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

    public String getFriendGroupTitle() {
        return friendGroupTitle;
    }

    public void setFriendGroupTitle(String friendGroupTitle) {
        this.friendGroupTitle = friendGroupTitle;
    }

}
