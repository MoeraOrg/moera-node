package org.moera.node.model.notification;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class PostingSubscriberNotification extends SubscriberNotification {

    private String postingId;

    protected PostingSubscriberNotification(NotificationType type) {
        super(type);
    }

    protected PostingSubscriberNotification(NotificationType type, String postingId) {
        super(type);
        this.postingId = postingId;
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
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
    }

}
