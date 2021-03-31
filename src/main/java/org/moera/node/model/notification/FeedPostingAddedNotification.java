package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class FeedPostingAddedNotification extends SubscriberNotification {

    private String feedName;
    private String postingId;

    public FeedPostingAddedNotification() {
        super(NotificationType.FEED_POSTING_ADDED);
    }

    public FeedPostingAddedNotification(String feedName, UUID postingId) {
        super(NotificationType.FEED_POSTING_ADDED);
        this.feedName = feedName;
        this.postingId = postingId.toString();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
    }

}
