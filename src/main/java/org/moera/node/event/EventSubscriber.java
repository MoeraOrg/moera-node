package org.moera.node.event;

public class EventSubscriber {

    private String sessionId;
    private int lastEventSeen;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getLastEventSeen() {
        return lastEventSeen;
    }

    public void setLastEventSeen(int lastEventSeen) {
        this.lastEventSeen = lastEventSeen;
    }

}
