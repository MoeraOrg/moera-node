package org.moera.node.friends;

import java.util.UUID;

public record FriendCacheInvalidation(
    FriendCachePart part,
    UUID nodeId,
    String clientName
) {
}
