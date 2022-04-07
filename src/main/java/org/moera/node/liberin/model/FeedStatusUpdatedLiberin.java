package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.FeedStatusChange;

public class FeedStatusUpdatedLiberin extends Liberin {

    private String feedName;
    private FeedStatus status;
    private FeedStatusChange change;

    public FeedStatusUpdatedLiberin(String feedName, FeedStatus status) {
        this(feedName, status, null);
    }

    public FeedStatusUpdatedLiberin(String feedName, FeedStatus status, FeedStatusChange change) {
        this.feedName = feedName;
        this.status = status;
        this.change = change;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public FeedStatus getStatus() {
        return status;
    }

    public void setStatus(FeedStatus status) {
        this.status = status;
    }

    public FeedStatusChange getChange() {
        return change;
    }

    public void setChange(FeedStatusChange change) {
        this.change = change;
    }

}
