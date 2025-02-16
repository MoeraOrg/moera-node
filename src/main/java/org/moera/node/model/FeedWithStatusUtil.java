package org.moera.node.model;

import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.FeedWithStatus;

public class FeedWithStatusUtil {
    
    public static FeedWithStatus build(String feedName, FeedStatus feedStatus) {
        FeedWithStatus feedWithStatus = new FeedWithStatus();
        feedWithStatus.setFeedName(feedName);
        feedWithStatus.setNotViewed(feedStatus.getNotViewed());
        feedWithStatus.setNotRead(feedStatus.getNotRead());
        feedWithStatus.setNotViewedMoment(feedStatus.getNotViewedMoment());
        feedWithStatus.setNotReadMoment(feedStatus.getNotReadMoment());
        return feedWithStatus;
    }

}
