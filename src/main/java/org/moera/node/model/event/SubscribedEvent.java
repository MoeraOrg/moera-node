package org.moera.node.model.event;

import org.moera.node.event.EventSubscriber;

public class SubscribedEvent extends Event {

    private String sessionId;
    private String clientIp;

    public SubscribedEvent() {
        super(EventType.SUBSCRIBED);
    }

    public SubscribedEvent(String sessionId, String clientIp) {
        this();
        this.sessionId = sessionId;
        this.clientIp = clientIp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.getSessionId().equals(sessionId);
    }

}
