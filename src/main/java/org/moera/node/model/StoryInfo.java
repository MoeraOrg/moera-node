package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryInfo {

    private String id;
    private String feedName;
    private String storyType;
    private long createdAt;
    private long publishedAt;
    private boolean pinned;
    private Long moment;
    private Boolean viewed;
    private Boolean read;
    private String summary;
    private Map<String, String[]> operations;

    public StoryInfo() {
    }

    protected StoryInfo(Story story, boolean isAdmin) {
        id = story.getId().toString();
        feedName = story.getFeedName();
        storyType = story.getStoryType().getValue();
        createdAt = Util.toEpochSecond(story.getCreatedAt());
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        pinned = story.isPinned();
        moment = story.getMoment();
        if (isAdmin) {
            viewed = story.isViewed();
            read = story.isRead();
        }
        summary = story.getSummary();
        operations = new HashMap<>();
        operations.put("edit", new String[]{"admin"});
        operations.put("delete", new String[]{"admin"});
    }

    public static StoryInfo build(Story story, boolean isAdmin,
                                  Function<Story, PostingInfo> buildPostingInfo) {
        switch (story.getStoryType()) {
            case POSTING_ADDED:
                return new StoryPostingAddedInfo(story, buildPostingInfo.apply(story), isAdmin);

            case REACTION_ADDED_POSITIVE:
            case REACTION_ADDED_NEGATIVE:
                return new StoryReactionAddedInfo(story, story.getEntry().getId(), isAdmin);

            default:
                return new StoryInfo(story, isAdmin);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getStoryType() {
        return storyType;
    }

    public void setStoryType(String storyType) {
        this.storyType = storyType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    public Boolean isViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public Boolean isRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
