package org.moera.node.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.moera.lib.node.types.FeedInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.model.FeedInfoUtil;

public class Feed {

    public static final String TIMELINE = "timeline";
    public static final String INSTANT = "instant";
    public static final String NEWS = "news";
    public static final String EXPLORE = "explore";

    private static final Map<String, FeedInfo> STANDARD = new HashMap<>();

    static {
        FeedInfo feedInfo = FeedInfoUtil.build(TIMELINE, Principal.ADMIN);
        feedInfo.setTitle("Timeline");
        STANDARD.put(TIMELINE, feedInfo);

        feedInfo = FeedInfoUtil.build(INSTANT, Principal.NONE);
        STANDARD.put(INSTANT, feedInfo);

        feedInfo = FeedInfoUtil.build(NEWS, Principal.NONE);
        feedInfo.setTitle("News");
        STANDARD.put(NEWS, feedInfo);

        feedInfo = FeedInfoUtil.build(EXPLORE, Principal.NONE);
        feedInfo.setTitle("Explore");
        STANDARD.put(EXPLORE, feedInfo);
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
        return !feedName.equals(TIMELINE);
    }

    public static boolean isReadable(String feedName, boolean isAdmin) {
        return isAdmin || !isAdmin(feedName);
    }

}
