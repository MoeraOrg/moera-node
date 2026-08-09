package org.moera.node.subscriptions;

import java.time.Instant;
import java.util.UUID;

record PendingSubscription(
    UUID id,
    UUID nodeId,
    Instant retryAt
) {
}
