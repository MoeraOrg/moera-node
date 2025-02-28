package org.moera.node.model.notification;

import java.sql.Timestamp;

import org.moera.lib.node.types.SearchEngine;
import org.moera.lib.node.types.notifications.SearchEngineClickedNotification;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.util.Util;

public class SearchEngineClickedNotificationUtil {

    public static SearchEngineClickedNotification build(
        SearchEngine searchEngine,
        String postingId,
        String commentId,
        String mediaId,
        String heading,
        Timestamp clickedAt
    ) {
        SearchEngineClickedNotification notification = new SearchEngineClickedNotification();

        notification.setSearchEngine(searchEngine);
        notification.setPostingId(postingId);
        notification.setCommentId(commentId);
        notification.setMediaId(mediaId);
        notification.setHeading(heading);
        notification.setClickedAt(Util.toEpochSecond(clickedAt));

        return notification;
    }

    public static SearchEngineClickedNotification build(SearchEngineStatistics statistics) {
        SearchEngineClickedNotification notification = new SearchEngineClickedNotification();

        notification.setSearchEngine(statistics.getEngine());
        notification.setPostingId(statistics.getPostingId());
        notification.setCommentId(statistics.getCommentId());
        notification.setMediaId(statistics.getMediaId());
        notification.setHeading(statistics.getHeading());
        notification.setClickedAt(Util.toEpochSecond(statistics.getClickedAt()));

        return notification;
    }

}
