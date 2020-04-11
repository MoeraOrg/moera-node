package org.moera.node.model;

import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class FeedReference {

    private String feedName;
    private Long publishedAt;
    private Long moment;
    private String storyId;

    public FeedReference() {
    }

    public FeedReference(Story story) {
        feedName = story.getFeedName();
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        moment = story.getMoment();
        storyId = story.getId().toString();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

}
