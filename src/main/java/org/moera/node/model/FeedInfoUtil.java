package org.moera.node.model;

import org.moera.lib.node.types.FeedInfo;
import org.moera.lib.node.types.FeedOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.option.Options;

public class FeedInfoUtil {

    public static FeedInfo build(String feedName, Principal addPrincipal) {
        FeedInfo feedInfo = new FeedInfo();
        feedInfo.setFeedName(feedName);
        FeedOperations operations = new FeedOperations();
        operations.setAdd(addPrincipal);
        feedInfo.setOperations(operations);
        return feedInfo;
    }

    public static void fillSheriffs(FeedInfo feedInfo, Options options) {
        feedInfo.setSheriffs(
            org.moera.node.operations.FeedOperations.getFeedSheriffs(options, feedInfo.getFeedName()).orElse(null)
        );
        feedInfo.setSheriffMarks(
            org.moera.node.operations.FeedOperations.getFeedSheriffMarks(options, feedInfo.getFeedName()).orElse(null)
        );
    }

}
