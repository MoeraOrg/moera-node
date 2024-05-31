package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.SheriffComplaintGroup;

public class SheriffOrderAttributes {

    private boolean delete;

    @NotBlank
    @Size(max = 63)
    private String feedName;

    private String postingId;

    private String commentId;

    private SheriffOrderCategory category;

    private SheriffOrderReason reasonCode;

    @Size(max = 4096)
    private String reasonDetails;

    public SheriffOrderAttributes() {
    }

    public SheriffOrderAttributes(SheriffComplaintGroup group, SheriffOrderCategory category,
                                  SheriffComplaintDecisionText decisionText) {
        delete = decisionText.isReject();
        feedName = group.getRemoteFeedName();
        postingId = group.getRemotePostingId();
        commentId = group.getRemoteCommentId();
        this.category = category;
        reasonCode = decisionText.getDecisionCode();
        if (reasonCode == null) {
            reasonCode = SheriffOrderReason.OTHER;
        }
        reasonDetails = decisionText.getDecisionDetails();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
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

    public SheriffOrderCategory getCategory() {
        return category;
    }

    public void setCategory(SheriffOrderCategory category) {
        this.category = category;
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

}
