package org.moera.node.data;

import java.util.UUID;

public record MediaPostingId(UUID postingId, UUID parentMediaId, UUID parentRemoteMediaId) {
}
