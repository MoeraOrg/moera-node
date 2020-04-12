package org.moera.node.model;

import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class StoryAttributes {

    private Long publishedAt;
    private Boolean pinned;
    private Boolean viewed;
    private Boolean read;

    public Long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Long publishedAt) {
        this.publishedAt = publishedAt;
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
        if (publishedAt != null) {
            story.setPublishedAt(Util.toTimestamp(publishedAt));
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
