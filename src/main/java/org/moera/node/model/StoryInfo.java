package org.moera.node.model;

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
    private Long moment;
    private Boolean viewed;
    private Boolean read;

    public StoryInfo() {
    }

    public StoryInfo(Story story, boolean isAdmin) {
        id = story.getId().toString();
        feedName = story.getFeedName();
        storyType = story.getStoryType().getValue();
        createdAt = Util.toEpochSecond(story.getCreatedAt());
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        moment = story.getMoment();
        if (isAdmin) {
            viewed = story.isViewed();
            read = story.isRead();
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

}
