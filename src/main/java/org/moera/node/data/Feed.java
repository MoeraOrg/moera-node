package org.moera.node.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.moera.node.model.FeedInfo;

public class Feed {

    public static final String TIMELINE = "timeline";
    public static final String INSTANT = "instant";
    public static final String NEWS = "news";

    private static final Map<String, FeedInfo> STANDARD = new HashMap<>();

    static {
        FeedInfo feedInfo = new FeedInfo(TIMELINE);
        feedInfo.setTitle("Timeline");
        feedInfo.setOperations(Collections.singletonMap("add", new String[]{"admin"}));
        STANDARD.put(TIMELINE, feedInfo);

        feedInfo = new FeedInfo(INSTANT);
        feedInfo.setOperations(Collections.singletonMap("add", new String[]{}));
        STANDARD.put(INSTANT, feedInfo);

        feedInfo = new FeedInfo(NEWS);
        feedInfo.setTitle("News");
        feedInfo.setOperations(Collections.singletonMap("add", new String[]{}));
        STANDARD.put(NEWS, feedInfo);
    }

    public static Collection<FeedInfo> getAllStandard(boolean isAdmin) {
        if (isAdmin) {
            return STANDARD.values();
        } else {
            return STANDARD.values().stream().filter(f -> !isAdmin(f.getFeedName())).collect(Collectors.toList());
        }
    }

    public static boolean isStandard(String feedName) {
        return STANDARD.containsKey(feedName);
    }

    public static FeedInfo getStandard(String feedName) {
        return STANDARD.get(feedName);
    }

    public static boolean isAdmin(String feedName) {
        return feedName.equals(INSTANT);
    }

    public static boolean isReadable(String feedName, boolean isAdmin) {
        return isAdmin || !isAdmin(feedName);
    }

}
