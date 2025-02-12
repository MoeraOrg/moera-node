package org.moera.node.notification.send;

import java.util.Objects;
import java.util.UUID;

import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class Direction {

    private final UUID nodeId;
    private final PrincipalFilter principalFilter;

    protected Direction() {
        this(null, Principal.PUBLIC);
    }

    protected Direction(UUID nodeId) {
        this(nodeId, Principal.PUBLIC);
    }

    protected Direction(UUID nodeId, PrincipalFilter principalFilter) {
        this.nodeId = nodeId;
        this.principalFilter = principalFilter;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public PrincipalFilter getPrincipalFilter() {
        return principalFilter;
    }

    public boolean isPermitted(boolean remoteIsSelf, String remoteNodeName, boolean subscribed,
                               String[] remoteFriendGroups) {
        return principalFilter.includes(remoteIsSelf, remoteNodeName, subscribed, remoteFriendGroups);
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (!(peer instanceof Direction direction)) {
            return false;
        }
        return Objects.equals(nodeId, direction.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

}
