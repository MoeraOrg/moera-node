package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryReaction {

    private String ownerName;
    private String ownerFullName;
    private int emoji;

    public StorySummaryReaction() {
    }

    public StorySummaryReaction(String ownerName, String ownerFullName, int emoji) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.emoji = emoji;
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

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

}
