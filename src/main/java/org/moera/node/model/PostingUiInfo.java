package org.moera.node.model;

import org.moera.lib.node.types.FeedReference;
import org.moera.lib.node.types.PostingInfo;
import org.moera.node.data.Feed;

public class PostingUiInfo extends PostingInfo {

    public String getSaneBodyPreview() {
        return PostingInfoUtil.getSaneBodyPreview(this);
    }

    public String getSaneBody() {
        return PostingInfoUtil.getSaneBody(this);
    }

    private FeedReference getFeedReference(String feedName) {
        if (getFeedReferences() == null) {
            return null;
        }
        return getFeedReferences().stream().filter(fr -> fr.getFeedName().equals(feedName)).findFirst().orElse(null);
    }

    public Long getTimelinePublishedAt() {
        FeedReference fr = getFeedReference(Feed.TIMELINE);
        return fr != null ? fr.getPublishedAt() : null;
    }

    public Boolean isTimelinePinned() {
        FeedReference fr = getFeedReference(Feed.TIMELINE);
        return fr != null ? fr.getPinned() : null;
    }

    public Long getTimelineMoment() {
        FeedReference fr = getFeedReference(Feed.TIMELINE);
        return fr != null ? fr.getMoment() : null;
    }

}
