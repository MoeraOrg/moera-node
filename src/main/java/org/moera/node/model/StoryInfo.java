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
    private Long moment;
    private boolean viewed;
    private boolean read;

    public StoryInfo() {
    }

    public StoryInfo(Story story) {
        id = story.getId().toString();
        feedName = story.getFeedName();
        storyType = story.getStoryType().getValue();
        createdAt = Util.toEpochSecond(story.getCreatedAt());
        moment = story.getMoment();
        viewed = story.isViewed();
        read = story.isRead();
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

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

}
