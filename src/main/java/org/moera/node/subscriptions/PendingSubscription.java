package org.moera.node.subscriptions;

import java.time.Instant;
import java.util.UUID;

class PendingSubscription {

    private final UUID id;
    private final UUID nodeId;
    private final Instant retryAt;

    PendingSubscription(UUID id, UUID nodeId, Instant retryAt) {
        this.id = id;
        this.nodeId = nodeId;
        this.retryAt = retryAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public Instant getRetryAt() {
        return retryAt;
    }

}
