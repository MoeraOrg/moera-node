package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class FeedSheriffDataUpdatedEvent extends Event {

    private String feedName;
    private List<String> sheriffs;
    private List<SheriffMark> sheriffMarks;

    public FeedSheriffDataUpdatedEvent() {
        super(EventType.FEED_SHERIFF_DATA_UPDATED, Scope.VIEW_CONTENT);
    }

    public FeedSheriffDataUpdatedEvent(String feedName, List<String> sheriffs, List<SheriffMark> sheriffMarks) {
        super(EventType.FEED_SHERIFF_DATA_UPDATED, Scope.VIEW_CONTENT);
        this.feedName = feedName;
        this.sheriffs = sheriffs;
        this.sheriffMarks = sheriffMarks;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public List<String> getSheriffs() {
        return sheriffs;
    }

    public void setSheriffs(List<String> sheriffs) {
        this.sheriffs = sheriffs;
    }

    public List<SheriffMark> getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(List<SheriffMark> sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
    }

}
