package org.moera.node.util;

import java.util.Objects;
import java.util.UUID;

public class Nodes {

    public UUID nodeId;
    public String remoteNodeName;

    public Nodes(UUID nodeId, String remoteNodeName) {
        this.nodeId = nodeId;
        this.remoteNodeName = remoteNodeName;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        Nodes nodes = (Nodes) peer;
        return nodeId.equals(nodes.nodeId) && remoteNodeName.equals(nodes.remoteNodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, remoteNodeName);
    }

}
