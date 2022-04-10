package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class RemotePostingMediaReactionAddedLiberin extends Liberin {

    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private String postingId;
    private String parentPostingId;
    private String parentMediaId;
    private String ownerName;
    private String ownerFullName;
    private AvatarImage ownerAvatar;
    private String postingHeading;
    private boolean negative;
    private int emoji;

    public RemotePostingMediaReactionAddedLiberin(String nodeName, String fullName, AvatarImage avatar,
                                                  String postingId, String parentPostingId, String parentMediaId,
                                                  String ownerName, String ownerFullName, AvatarImage ownerAvatar,
                                                  String postingHeading, boolean negative, int emoji) {
        this.nodeName = nodeName;
        this.fullName = fullName;
        this.avatar = avatar;
        this.postingId = postingId;
        this.parentPostingId = parentPostingId;
        this.parentMediaId = parentMediaId;
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerAvatar = ownerAvatar;
        this.postingHeading = postingHeading;
        this.negative = negative;
        this.emoji = emoji;
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

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getParentPostingId() {
        return parentPostingId;
    }

    public void setParentPostingId(String parentPostingId) {
        this.parentPostingId = parentPostingId;
    }

    public String getParentMediaId() {
        return parentMediaId;
    }

    public void setParentMediaId(String parentMediaId) {
        this.parentMediaId = parentMediaId;
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

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

}