package org.moera.node.liberin.model;

import java.util.Map;
import java.util.Set;

import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.FeedStatusChange;
import org.moera.node.data.Story;
import org.moera.node.liberin.Liberin;

public class FeedStatusUpdatedLiberin extends Liberin {

    private String feedName;
    private FeedStatus status;
    private FeedStatusChange change;
    private Set<Story> instantsUpdated;

    public FeedStatusUpdatedLiberin(String feedName, FeedStatus status) {
        this(feedName, status, null, null);
    }

    public FeedStatusUpdatedLiberin(
        String feedName, FeedStatus status, FeedStatusChange change, Set<Story> instantsUpdated
    ) {
        this.feedName = feedName;
        this.status = status;
        this.change = change;
        this.instantsUpdated = instantsUpdated;
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

    public Set<Story> getInstantsUpdated() {
        return instantsUpdated;
    }

    public void setInstantsUpdated(Set<Story> instantsUpdated) {
        this.instantsUpdated = instantsUpdated;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("feedName", feedName);
        model.put("status", status);
        model.put("change", change);
        model.put("instantsUpdated", instantsUpdated);
    }

}
