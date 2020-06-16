package org.moera.node.model.notification;

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

}
