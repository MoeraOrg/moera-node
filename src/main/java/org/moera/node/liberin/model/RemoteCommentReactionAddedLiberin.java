package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class RemoteCommentReactionAddedLiberin extends Liberin {

    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private String postingId;
    private String commentId;
    private String ownerName;
    private String ownerFullName;
    private AvatarImage ownerAvatar;
    private String commentHeading;
    private boolean negative;
    private int emoji;

    public RemoteCommentReactionAddedLiberin(String nodeName, String fullName, AvatarImage avatar, String postingId,
                                             String commentId, String ownerName, String ownerFullName,
                                             AvatarImage ownerAvatar, String commentHeading, boolean negative,
                                             int emoji) {
        this.nodeName = nodeName;
        this.fullName = fullName;
        this.avatar = avatar;
        this.postingId = postingId;
        this.commentId = commentId;
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerAvatar = ownerAvatar;
        this.commentHeading = commentHeading;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
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

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("fullName", fullName);
        model.put("avatar", avatar);
        model.put("postingId", postingId);
        model.put("commentId", commentId);
        model.put("ownerName", ownerName);
        model.put("ownerFullName", ownerFullName);
        model.put("ownerAvatar", ownerAvatar);
        model.put("commentHeading", commentHeading);
        model.put("negative", negative);
        model.put("emoji", emoji);
    }

}
