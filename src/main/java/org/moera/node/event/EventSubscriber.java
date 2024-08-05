package org.moera.node.event;

import java.util.UUID;

import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.option.Options;

public class EventSubscriber implements AccessChecker {

    private UUID nodeId;
    private Options options;
    private String sessionId;
    private int lastEventSeen;
    private boolean admin;
    private long authScope;
    private String clientName;
    private boolean subscribedToClient;
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

    public boolean isAdmin(Scope scope) {
        return admin && hasAuthScope(scope);
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public long getAuthScope() {
        return authScope;
    }

    public void setAuthScope(long authScope) {
        this.authScope = authScope;
    }

    public boolean hasAuthScope(Scope scope) {
        return (this.authScope & scope.getMask()) == scope.getMask();
    }

    public String getClientName(Scope scope) {
        return hasAuthScope(scope) ? (admin ? nodeName() : clientName) : null;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public boolean isSubscribedToClient(Scope scope) {
        return subscribedToClient && hasAuthScope(scope);
    }

    public void setSubscribedToClient(boolean subscribedToClient) {
        this.subscribedToClient = subscribedToClient;
    }

    public String[] getFriendGroups(Scope scope) {
        return hasAuthScope(scope) ? friendGroups : new String[0];
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
    public boolean isPrincipal(PrincipalFilter principal, Scope scope) {
        return principal.includes(
                isAdmin(scope), getClientName(scope), isSubscribedToClient(scope), getFriendGroups(scope));
    }

    public String nodeName() {
        return options != null ? options.nodeName() : null;
    }

}
