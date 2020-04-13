package org.moera.node.model;

import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class StoryAttributes {

    private String feedName;
    private Long publishAt;
    private Boolean pinned;
    private Boolean viewed;
    private Boolean read;

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Long getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Long publishAt) {
        this.publishAt = publishAt;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public void toStory(Story story) {
        if (feedName != null) {
            story.setFeedName(feedName);
        }
        if (publishAt != null) {
            story.setPublishedAt(Util.toTimestamp(publishAt));
        }
        if (pinned != null) {
            story.setPinned(pinned);
        }
        if (viewed != null) {
            story.setViewed(viewed);
        }
        if (read != null) {
            story.setRead(read);
        }
    }

}
