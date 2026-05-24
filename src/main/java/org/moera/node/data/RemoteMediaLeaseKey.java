package org.moera.node.data;

import java.util.UUID;

public record RemoteMediaLeaseKey(UUID nodeId, String remoteNodeName, String leaseId) {
}
