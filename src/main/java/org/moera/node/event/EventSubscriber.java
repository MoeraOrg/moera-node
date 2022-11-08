package org.moera.node.event;

import java.util.UUID;

public class EventSubscriber {

    private UUID nodeId;
    private String sessionId;
    private int lastEventSeen;
    private boolean admin;
    private String clientName;
    private String[] friendGroups;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String[] getFriendGroups() {
        return friendGroups;
    }

    public void setFriendGroups(String[] friendGroups) {
        this.friendGroups = friendGroups;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

}
