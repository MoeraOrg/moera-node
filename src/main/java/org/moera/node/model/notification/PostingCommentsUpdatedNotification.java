package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class PostingCommentsUpdatedNotification extends PostingSubscriberNotification {

    private int total;

    public PostingCommentsUpdatedNotification() {
        super(NotificationType.POSTING_COMMENTS_UPDATED);
    }

    public PostingCommentsUpdatedNotification(UUID postingId, int total) {
        super(NotificationType.POSTING_COMMENTS_UPDATED, postingId.toString());
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("total", LogUtil.format(total)));
    }

}
