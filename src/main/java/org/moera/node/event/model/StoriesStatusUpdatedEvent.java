package org.moera.node.event.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.FeedStatusChange;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoriesStatusUpdatedEvent extends Event {

    private String feedName;
    private Boolean viewed;
    private Boolean read;
    private long before;

    public StoriesStatusUpdatedEvent() {
        super(EventType.STORIES_STATUS_UPDATED);
    }

    public StoriesStatusUpdatedEvent(String feedName, FeedStatusChange change) {
        super(EventType.STORIES_STATUS_UPDATED);

        this.feedName = feedName;
        viewed = change.getViewed();
        read = change.getRead();
        before = change.getBefore();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
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

    public long getBefore() {
        return before;
    }

    public void setBefore(long before) {
        this.before = before;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
