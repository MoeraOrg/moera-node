package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class ReplyCommentAddedLiberin extends Liberin {

    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private String postingId;
    private String postingHeading;
    private String commentId;
    private String repliedToId;
    private String repliedToHeading;
    private String commentOwnerName;
    private String commentOwnerFullName;
    private AvatarImage commentOwnerAvatar;

    public ReplyCommentAddedLiberin(String nodeName, String fullName, AvatarImage avatar, String postingId,
                                    String postingHeading, String commentId, String repliedToId,
                                    String repliedToHeading, String commentOwnerName, String commentOwnerFullName,
                                    AvatarImage commentOwnerAvatar) {
        this.nodeName = nodeName;
        this.fullName = fullName;
        this.avatar = avatar;
        this.postingId = postingId;
        this.postingHeading = postingHeading;
        this.commentId = commentId;
        this.repliedToId = repliedToId;
        this.repliedToHeading = repliedToHeading;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentOwnerAvatar = commentOwnerAvatar;
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

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(String repliedToId) {
        this.repliedToId = repliedToId;
    }

    public String getRepliedToHeading() {
        return repliedToHeading;
    }

    public void setRepliedToHeading(String repliedToHeading) {
        this.repliedToHeading = repliedToHeading;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerFullName() {
        return commentOwnerFullName;
    }

    public void setCommentOwnerFullName(String commentOwnerFullName) {
        this.commentOwnerFullName = commentOwnerFullName;
    }

    public AvatarImage getCommentOwnerAvatar() {
        return commentOwnerAvatar;
    }

    public void setCommentOwnerAvatar(AvatarImage commentOwnerAvatar) {
        this.commentOwnerAvatar = commentOwnerAvatar;
    }

}
