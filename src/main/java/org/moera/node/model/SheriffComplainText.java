package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainGroup;

public class SheriffComplainText {

    @Size(max = 96)
    private String ownerFullName;

    @Size(max = 31)
    private String ownerGender;

    @NotBlank
    @Size(max = 63)
    private String nodeName;

    @Size(max = 96)
    private String fullName;

    @NotBlank
    @Size(max = 63)
    private String feedName;

    @Size(max = 63)
    private String postingOwnerName;

    @Size(max = 96)
    private String postingOwnerFullName;

    @Size(max = 31)
    private String postingOwnerGender;

    @Size(max = 255)
    private String postingHeading;

    @Size(max = 40)
    private String postingId;

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    @Size(max = 31)
    private String commentOwnerGender;

    @Size(max = 255)
    private String commentHeading;

    @Size(max = 40)
    private String commentId;

    private SheriffOrderReason reasonCode;

    @Size(max = 4096)
    private String reasonDetails;

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

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
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

    public String getPostingOwnerGender() {
        return postingOwnerGender;
    }

    public void setPostingOwnerGender(String postingOwnerGender) {
        this.postingOwnerGender = postingOwnerGender;
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

    public String getCommentOwnerGender() {
        return commentOwnerGender;
    }

    public void setCommentOwnerGender(String commentOwnerGender) {
        this.commentOwnerGender = commentOwnerGender;
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

    public SheriffOrderReason getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(SheriffOrderReason reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDetails() {
        return reasonDetails;
    }

    public void setReasonDetails(String reasonDetails) {
        this.reasonDetails = reasonDetails;
    }

    public void toSheriffComplain(SheriffComplain sheriffComplain) {
        sheriffComplain.setOwnerFullName(ownerFullName);
        sheriffComplain.setOwnerGender(ownerGender);
        sheriffComplain.setReasonCode(reasonCode != null ? reasonCode : SheriffOrderReason.OTHER);
        sheriffComplain.setReasonDetails(reasonDetails);
    }

    public void toSheriffComplainGroup(SheriffComplainGroup sheriffComplainGroup) {
        sheriffComplainGroup.setRemoteNodeName(nodeName);
        sheriffComplainGroup.setRemoteNodeFullName(fullName);
        sheriffComplainGroup.setRemoteFeedName(feedName);
        sheriffComplainGroup.setRemotePostingOwnerName(postingOwnerName);
        sheriffComplainGroup.setRemotePostingOwnerFullName(postingOwnerFullName);
        sheriffComplainGroup.setRemotePostingOwnerGender(postingOwnerGender);
        sheriffComplainGroup.setRemotePostingHeading(postingHeading);
        sheriffComplainGroup.setRemotePostingId(postingId);
        sheriffComplainGroup.setRemoteCommentOwnerName(commentOwnerName);
        sheriffComplainGroup.setRemoteCommentOwnerFullName(commentOwnerFullName);
        sheriffComplainGroup.setRemoteCommentOwnerGender(commentOwnerGender);
        sheriffComplainGroup.setRemoteCommentHeading(commentHeading);
        sheriffComplainGroup.setRemoteCommentId(commentId);
    }

}
