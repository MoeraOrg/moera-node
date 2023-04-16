package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.SheriffMark;
import org.springframework.data.util.Pair;

public class FeedSheriffDataUpdatedEvent extends Event {

    private String feedName;
    private List<String> sheriffs;
    private SheriffMark[] sheriffMarks;

    public FeedSheriffDataUpdatedEvent() {
        super(EventType.FEED_SHERIFF_DATA_UPDATED);
    }

    public FeedSheriffDataUpdatedEvent(String feedName, List<String> sheriffs, SheriffMark[] sheriffMarks) {
        super(EventType.FEED_SHERIFF_DATA_UPDATED);
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

    public SheriffMark[] getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(SheriffMark[] sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
    }

}
