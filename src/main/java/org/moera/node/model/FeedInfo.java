package org.moera.node.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.moera.node.data.Feed;

public class FeedInfo {

    private static final Map<String, FeedInfo> STANDARD = new HashMap<>();

    static {
        FeedInfo feedInfo = new FeedInfo(Feed.TIMELINE);
        feedInfo.setOperations(Collections.singletonMap("add", new String[]{"admin"}));
        STANDARD.put(Feed.TIMELINE, feedInfo);

        feedInfo = new FeedInfo(Feed.INSTANT);
        feedInfo.setOperations(Collections.singletonMap("add", new String[]{}));
        STANDARD.put(Feed.INSTANT, feedInfo);
    }

    private String feedName;
    private Map<String, String[]> operations;

    public FeedInfo() {
    }

    public FeedInfo(String feedName) {
        this.feedName = feedName;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

    public static Collection<FeedInfo> getAllStandard() {
        return STANDARD.values();
    }

    public static boolean isStandard(String feedName) {
        return STANDARD.containsKey(feedName);
    }

    public static FeedInfo getStandard(String feedName) {
        return STANDARD.get(feedName);
    }

}
