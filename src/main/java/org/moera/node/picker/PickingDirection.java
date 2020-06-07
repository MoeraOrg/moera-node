package org.moera.node.picker;

import java.util.Objects;
import java.util.UUID;

class PickingDirection {

    private UUID nodeId;
    private String nodeName;

    PickingDirection(UUID nodeId, String nodeName) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        PickingDirection direction = (PickingDirection) peer;
        return nodeId.equals(direction.nodeId) && nodeName.equals(direction.nodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeName);
    }

}
