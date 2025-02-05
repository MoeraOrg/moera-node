package org.moera.node.model.notification;

import java.util.List;

import javax.validation.constraints.Size;

import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class PostingSubscriberNotification extends SubscriberNotification {

    @Size(max = 36)
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
