package org.moera.node.liberin.model;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.SheriffComplaintDecidedNotification;
import org.moera.node.liberin.Liberin;

public class RemoteSheriffComplaintDecidedLiberin extends Liberin {

    private String remoteNodeName;
    private String remoteFeedName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingOwnerSourceUri;
    private String postingHeading;
    private String postingId;
    private String commentOwnerName;
    private String commentOwnerFullName;
    private String commentOwnerSourceUri;
    private String commentHeading;
    private String commentId;
    private String sheriffName;
    private AvatarImage sheriffAvatar;
    private String complaintGroupId;

    public RemoteSheriffComplaintDecidedLiberin(SheriffComplaintDecidedNotification notification) {
        remoteNodeName = notification.getRemoteNodeName();
        remoteFeedName = notification.getRemoteFeedName();
        postingOwnerName = notification.getPostingOwnerName();
        postingOwnerFullName = notification.getPostingOwnerFullName();
        postingOwnerSourceUri = notification.getPostingOwnerSourceUri();
        postingHeading = notification.getPostingHeading();
        postingId = notification.getPostingId();
        commentHeading = notification.getCommentHeading();
        commentId = notification.getCommentId();
        sheriffName = notification.getSenderNodeName();
        sheriffAvatar = notification.getSenderAvatar();
        complaintGroupId = notification.getComplaintGroupId();
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public String getPostingOwnerFullName() {
        return postingOwnerFullName;
    }

    public void setPostingOwnerFullName(String postingOwnerFullName) {
        this.postingOwnerFullName = postingOwnerFullName;
    }

    public String getPostingOwnerSourceUri() {
        return postingOwnerSourceUri;
    }

    public void setPostingOwnerSourceUri(String postingOwnerSourceUri) {
        this.postingOwnerSourceUri = postingOwnerSourceUri;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
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

    public String getCommentOwnerSourceUri() {
        return commentOwnerSourceUri;
    }

    public void setCommentOwnerSourceUri(String commentOwnerSourceUri) {
        this.commentOwnerSourceUri = commentOwnerSourceUri;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getSheriffName() {
        return sheriffName;
    }

    public void setSheriffName(String sheriffName) {
        this.sheriffName = sheriffName;
    }

    public AvatarImage getSheriffAvatar() {
        return sheriffAvatar;
    }

    public void setSheriffAvatar(AvatarImage sheriffAvatar) {
        this.sheriffAvatar = sheriffAvatar;
    }

    public String getComplaintGroupId() {
        return complaintGroupId;
    }

    public void setComplaintGroupId(String complaintGroupId) {
        this.complaintGroupId = complaintGroupId;
    }

}
