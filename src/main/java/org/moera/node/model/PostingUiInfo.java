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

    private record FeedReferenceSummary(boolean pinned, long moment) {
    }

    private FeedReferenceSummary getFeedReferenceSummary(String feedName) {
        if (getFeedReferences() == null) {
            return null;
        }

        FeedReference latest = null;
        boolean pinned = false;
        for (var fr : getFeedReferences()) {
            if (!fr.getFeedName().equals(feedName)) {
                continue;
            }
            if (latest == null || fr.getPublishedAt() > latest.getPublishedAt()) {
                latest = fr;
            }
            pinned = pinned || fr.getPinned();
        }

        return latest != null ? new FeedReferenceSummary(pinned, latest.getMoment()) : null;
    }

    public Long getTimelinePublishedAt() {
        Long publishedAt = getEarliestPublishedAt();
        return publishedAt != null ? publishedAt : getCreatedAt();
    }

    public Boolean isTimelinePinned() {
        var frs = getFeedReferenceSummary(Feed.TIMELINE);
        return frs != null ? frs.pinned : null;
    }

    public Long getTimelineMoment() {
        var frs = getFeedReferenceSummary(Feed.TIMELINE);
        return frs != null ? frs.moment : null;
    }

}
