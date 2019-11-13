package org.moera.node.event;

import java.util.UUID;

public class EventSubscriber {

    private UUID nodeId;
    private String sessionId;
    private int lastEventSeen;
    private boolean admin;
    private boolean subscribed;

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

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

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

}
