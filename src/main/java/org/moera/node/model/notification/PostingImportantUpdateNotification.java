package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class PostingImportantUpdateNotification extends PostingSubscriberNotification {

    private String postingHeading;
    private String description;

    public PostingImportantUpdateNotification() {
        super(NotificationType.POSTING_IMPORTANT_UPDATE);
    }

    public PostingImportantUpdateNotification(UUID postingId, String postingHeading, String description) {
        super(NotificationType.POSTING_IMPORTANT_UPDATE, postingId.toString());
        this.postingHeading = postingHeading;
        this.description = description;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("postingHeading", LogUtil.format(postingHeading)));
        parameters.add(Pair.of("description", LogUtil.format(description)));
    }

}
