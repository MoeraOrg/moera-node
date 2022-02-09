package org.moera.node.notification.send;

import java.util.Objects;
import java.util.UUID;

public class Direction {

    private UUID nodeId;

    public Direction() {
    }

    public Direction(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (!(peer instanceof Direction)) {
            return false;
        }
        Direction direction = (Direction) peer;
        return Objects.equals(nodeId, direction.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

}
