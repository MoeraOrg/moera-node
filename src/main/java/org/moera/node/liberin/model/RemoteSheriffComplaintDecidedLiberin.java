package org.moera.node.liberin.model;

import java.util.Map;
import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.SheriffComplaintDecidedNotification;
import org.moera.node.liberin.Liberin;

public class RemoteSheriffComplaintDecidedLiberin extends Liberin {

    private String remoteNodeName;
    private String remoteFeedName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingHeading;
    private String postingId;
    private String commentOwnerName;
    private String commentOwnerFullName;
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

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        model.put("remoteNodeName", remoteNodeName);
        model.put("remoteFeedName", remoteFeedName);
        model.put("postingOwnerName", postingOwnerName);
        model.put("postingOwnerFullName", postingOwnerFullName);
        model.put("postingHeading", postingHeading);
        model.put("postingId", postingId);
        model.put("commentOwnerName", commentOwnerName);
        model.put("commentOwnerFullName", commentOwnerFullName);
        model.put("commentHeading", commentHeading);
        model.put("commentId", commentId);
        model.put("sheriffName", sheriffName);
        model.put("complaintGroupId", complaintGroupId);
    }

}
