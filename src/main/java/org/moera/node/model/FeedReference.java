package org.moera.node.model;

import org.moera.node.data.Story;

public class FeedReference {

    private String feedName;
    private Long moment;
    private String storyId;

    public FeedReference() {
    }

    public FeedReference(Story story) {
        feedName = story.getFeedName();
        moment = story.getMoment();
        storyId = story.getId().toString();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
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
