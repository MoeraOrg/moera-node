package org.moera.node.friends;

import java.util.UUID;

public class FriendCacheInvalidation {

    private final FriendCachePart part;
    private final UUID nodeId;
    private final String clientName;

    public FriendCacheInvalidation(FriendCachePart part, UUID nodeId, String clientName) {
        this.part = part;
        this.nodeId = nodeId;
        this.clientName = clientName;
    }

    public FriendCachePart getPart() {
        return part;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getClientName() {
        return clientName;
    }

}
