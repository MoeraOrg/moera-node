package org.moera.node.model.notification;

import java.util.UUID;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.notifications.PostingReactionsUpdatedNotification;

public class PostingReactionsUpdatedNotificationUtil {
    
    public static PostingReactionsUpdatedNotification build(UUID postingId, ReactionTotalsInfo totals) {
        PostingReactionsUpdatedNotification notification = new PostingReactionsUpdatedNotification();
        notification.setPostingId(postingId.toString());
        notification.setTotals(totals);
        return notification;
    }

}
