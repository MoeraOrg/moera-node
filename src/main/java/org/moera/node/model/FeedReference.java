package org.moera.node.model;

import org.moera.node.data.Story;

public class FeedReference {

    private String feedName;
    private Long moment;

    public FeedReference() {
    }

    public FeedReference(Story story) {
        feedName = story.getFeedName();
        moment = story.getMoment();
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

}
