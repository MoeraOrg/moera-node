package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;

public class SheriffComplaintText {

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

    private Boolean anonymous;

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

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public void toSheriffComplaint(SheriffComplaint sheriffComplaint) {
        sheriffComplaint.setOwnerFullName(ownerFullName);
        sheriffComplaint.setOwnerGender(ownerGender);
        sheriffComplaint.setReasonCode(reasonCode != null ? reasonCode : SheriffOrderReason.OTHER);
        sheriffComplaint.setReasonDetails(reasonDetails);
        if (anonymous != null) {
            sheriffComplaint.setAnonymousRequested(anonymous);
        }
    }

    public void toSheriffComplaintGroup(SheriffComplaintGroup sheriffComplaintGroup) {
        sheriffComplaintGroup.setRemoteNodeName(nodeName);
        sheriffComplaintGroup.setRemoteNodeFullName(fullName);
        sheriffComplaintGroup.setRemoteFeedName(feedName);
        sheriffComplaintGroup.setRemotePostingOwnerName(postingOwnerName);
        sheriffComplaintGroup.setRemotePostingOwnerFullName(postingOwnerFullName);
        sheriffComplaintGroup.setRemotePostingOwnerGender(postingOwnerGender);
        sheriffComplaintGroup.setRemotePostingHeading(postingHeading);
        sheriffComplaintGroup.setRemotePostingId(postingId);
        sheriffComplaintGroup.setRemoteCommentOwnerName(commentOwnerName);
        sheriffComplaintGroup.setRemoteCommentOwnerFullName(commentOwnerFullName);
        sheriffComplaintGroup.setRemoteCommentOwnerGender(commentOwnerGender);
        sheriffComplaintGroup.setRemoteCommentHeading(commentHeading);
        sheriffComplaintGroup.setRemoteCommentId(commentId);
    }

}
