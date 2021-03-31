package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class MentionPostingNotification extends Notification {

    private String postingId;

    protected MentionPostingNotification(NotificationType type) {
        super(type);
    }

    public MentionPostingNotification(NotificationType type, UUID postingId) {
        super(type);
        this.postingId = postingId.toString();
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
