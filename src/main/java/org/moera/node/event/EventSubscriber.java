package org.moera.node.event;

import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.option.Options;

public class EventSubscriber implements AccessChecker {

    private UUID nodeId;
    private Options options;
    private String sessionId;
    private int lastEventSeen;
    private long adminScope;
    private long clientScope;
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
        return scope.included(adminScope);
    }

    public long getAdminScope() {
        return adminScope;
    }

    public void setAdminScope(long adminScope) {
        this.adminScope = adminScope;
    }

    public long getClientScope() {
        return clientScope;
    }

    public void setClientScope(long clientScope) {
        this.clientScope = clientScope;
    }

    public boolean hasClientScope(Scope scope) {
        return scope.included(clientScope);
    }

    public String getClientName(Scope scope) {
        return hasClientScope(scope) ? clientName : null;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public boolean isSubscribedToClient(Scope scope) {
        return subscribedToClient && hasClientScope(scope);
    }

    public void setSubscribedToClient(boolean subscribedToClient) {
        this.subscribedToClient = subscribedToClient;
    }

    public String[] getFriendGroups(Scope scope) {
        return hasClientScope(scope) ? friendGroups : new String[0];
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
            isAdmin(scope), getClientName(scope), isSubscribedToClient(scope), getFriendGroups(scope)
        );
    }

    public String nodeName() {
        return options != null ? options.nodeName() : null;
    }

}
