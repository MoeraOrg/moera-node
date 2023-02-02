package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SubscriptionReason;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryData {

    private StorySummaryNode node;
    private StorySummaryEntry posting;
    private StorySummaryEntry comment;
    private List<StorySummaryEntry> comments;
    private Integer totalComments;
    private StorySummaryEntry repliedTo;
    private StorySummaryEntry parentPosting;
    private StorySummaryReaction reaction;
    private List<StorySummaryReaction> reactions;
    private Integer totalReactions;
    private String feedName;
    private SubscriptionReason subscriptionReason;
    private StorySummaryFriendGroup friendGroup;
    private StorySummaryBlocked blocked;
    private String description;

    public StorySummaryNode getNode() {
        return node;
    }

    public void setNode(StorySummaryNode node) {
        this.node = node;
    }

    public StorySummaryEntry getPosting() {
        return posting;
    }

    public void setPosting(StorySummaryEntry posting) {
        this.posting = posting;
    }

    public StorySummaryEntry getComment() {
        return comment;
    }

    public void setComment(StorySummaryEntry comment) {
        this.comment = comment;
    }

    public List<StorySummaryEntry> getComments() {
        return comments;
    }

    public void setComments(List<StorySummaryEntry> comments) {
        this.comments = comments;
    }

    public Integer getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(Integer totalComments) {
        this.totalComments = totalComments;
    }

    public StorySummaryEntry getRepliedTo() {
        return repliedTo;
    }

    public void setRepliedTo(StorySummaryEntry repliedTo) {
        this.repliedTo = repliedTo;
    }

    public StorySummaryEntry getParentPosting() {
        return parentPosting;
    }

    public void setParentPosting(StorySummaryEntry parentPosting) {
        this.parentPosting = parentPosting;
    }

    public StorySummaryReaction getReaction() {
        return reaction;
    }

    public void setReaction(StorySummaryReaction reaction) {
        this.reaction = reaction;
    }

    public List<StorySummaryReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<StorySummaryReaction> reactions) {
        this.reactions = reactions;
    }

    public Integer getTotalReactions() {
        return totalReactions;
    }

    public void setTotalReactions(Integer totalReactions) {
        this.totalReactions = totalReactions;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public SubscriptionReason getSubscriptionReason() {
        return subscriptionReason;
    }

    public void setSubscriptionReason(SubscriptionReason subscriptionReason) {
        this.subscriptionReason = subscriptionReason;
    }

    public StorySummaryFriendGroup getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(StorySummaryFriendGroup friendGroup) {
        this.friendGroup = friendGroup;
    }

    public StorySummaryBlocked getBlocked() {
        return blocked;
    }

    public void setBlocked(StorySummaryBlocked blocked) {
        this.blocked = blocked;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
