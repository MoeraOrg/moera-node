package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.FeedStatusChange;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoriesStatusUpdatedEvent extends Event {

    private String feedName;
    private Boolean viewed;
    private Boolean read;
    private long before;

    public StoriesStatusUpdatedEvent() {
        super(EventType.STORIES_STATUS_UPDATED, Scope.VIEW_FEEDS, Principal.ADMIN);
    }

    public StoriesStatusUpdatedEvent(String feedName, FeedStatusChange change) {
        super(EventType.STORIES_STATUS_UPDATED, Scope.VIEW_FEEDS, Principal.ADMIN);

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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("viewed", LogUtil.format(viewed)));
        parameters.add(Pair.of("read", LogUtil.format(read)));
        parameters.add(Pair.of("before", LogUtil.format(before)));
    }

}
