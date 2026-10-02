package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.EntryType;
import org.moera.node.data.Story;
import org.moera.node.liberin.Liberin;

public class StoryDeletedLiberin extends Liberin {

    private UUID id;
    private StoryType storyType;
    private String feedName;
    private long moment;
    private UUID postingId;
    private String receiverName;
    private String receiverPostingId;
    private String postingOwnerName;
    private UUID commentId;
    private PrincipalFilter viewFilter;
    private boolean postingDeleted;

    public StoryDeletedLiberin(Story story) {
        this(story, false);
    }

    public StoryDeletedLiberin(Story story, boolean postingDeleted) {
        id = story.getId();
        storyType = story.getStoryType();
        feedName = story.getFeedName();
        moment = story.getMoment();
        if (story.getEntry() != null) {
            if (story.getEntry().getEntryType() == EntryType.COMMENT) {
                postingId = story.getEntry().getParent().getId();
                commentId = story.getEntry().getId();
            } else {
                postingId = story.getEntry().getId();
            }
        }
        if (story.getEntry() != null) {
            receiverName = story.getEntry().getReceiverName();
            receiverPostingId = story.getEntry().getReceiverEntryId();
            postingOwnerName = story.getEntry().getOwnerName();
        }
        viewFilter = story.getViewPrincipalFilter();
        this.postingDeleted = postingDeleted;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPostingId() {
        return receiverPostingId;
    }

    public void setReceiverPostingId(String receiverPostingId) {
        this.receiverPostingId = receiverPostingId;
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }

    public PrincipalFilter getViewFilter() {
        return viewFilter;
    }

    public void setViewFilter(PrincipalFilter viewFilter) {
        this.viewFilter = viewFilter;
    }

    public boolean isPostingDeleted() {
        return postingDeleted;
    }

    public void setPostingDeleted(boolean postingDeleted) {
        this.postingDeleted = postingDeleted;
    }

}
