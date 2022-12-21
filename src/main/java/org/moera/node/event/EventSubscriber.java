package org.moera.node.event;

import java.util.UUID;

import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.option.Options;

public class EventSubscriber implements AccessChecker {

    private UUID nodeId;
    private Options options;
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

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
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

    @Override
    public boolean isPrincipal(PrincipalFilter principal) {
        return principal.includes(admin, clientName, friendGroups);
    }

}
