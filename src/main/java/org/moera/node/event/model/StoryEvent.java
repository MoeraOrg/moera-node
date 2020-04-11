package org.moera.node.event.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryEvent extends Event {

    private String id;
    private String storyType;
    private String feedName;
    private long publishedAt;
    private long moment;
    private String postingId;

    public StoryEvent(EventType type) {
        super(type);
    }

    public StoryEvent(EventType type, Story story) {
        super(type);
        id = story.getId().toString();
        storyType = story.getStoryType().getValue();
        feedName = story.getFeedName();
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        moment = story.getMoment();
        postingId = story.getEntry() != null ? story.getEntry().getId().toString() : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoryType() {
        return storyType;
    }

    public void setStoryType(String storyType) {
        this.storyType = storyType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
