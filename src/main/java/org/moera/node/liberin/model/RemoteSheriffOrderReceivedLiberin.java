package org.moera.node.liberin.model;

import java.util.Map;
import javax.persistence.EntityManager;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.SheriffOrderForCommentNotification;
import org.moera.node.model.notification.SheriffOrderForPostingNotification;

public class RemoteSheriffOrderReceivedLiberin extends Liberin {

    private boolean deleted;
    private String remoteNodeName;
    private String remoteFeedName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingHeading;
    private String postingId;
    private String commentHeading;
    private String commentId;
    private String sheriffName;
    private AvatarImage sheriffAvatar;
    private String orderId;

    public RemoteSheriffOrderReceivedLiberin(boolean deleted, SheriffOrderForPostingNotification notification) {
        this.deleted = deleted;
        remoteNodeName = notification.getRemoteNodeName();
        remoteFeedName = notification.getRemoteFeedName();
        postingHeading = notification.getPostingHeading();
        postingId = notification.getPostingId();
        sheriffName = notification.getSenderNodeName();
        sheriffAvatar = notification.getSenderAvatar();
        orderId = notification.getOrderId();
    }

    public RemoteSheriffOrderReceivedLiberin(boolean deleted, SheriffOrderForCommentNotification notification) {
        this.deleted = deleted;
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
        orderId = notification.getOrderId();
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        model.put("deleted", deleted);
        model.put("remoteNodeName", remoteNodeName);
        model.put("remoteFeedName", remoteFeedName);
        model.put("postingOwnerName", postingOwnerName);
        model.put("postingOwnerFullName", postingOwnerFullName);
        model.put("postingHeading", postingHeading);
        model.put("postingId", postingId);
        model.put("commentHeading", commentHeading);
        model.put("commentId", commentId);
        model.put("sheriffName", sheriffName);
        model.put("orderId", orderId);
    }

}
