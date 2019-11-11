package org.moera.node.event.model;

import org.moera.node.event.EventSubscriber;

public class SubscribedEvent extends Event {

    private String sessionId;

    public SubscribedEvent() {
        super(EventType.SUBSCRIBED);
    }

    public SubscribedEvent(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.getSessionId().equals(sessionId);
    }

}
