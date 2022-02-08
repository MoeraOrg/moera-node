package org.moera.node.notification.send;

import java.util.Objects;
import java.util.UUID;

class SingleDirection extends Direction {

    private String nodeName;

    SingleDirection(UUID nodeId, String nodeName) {
        super(nodeId);
        this.nodeName = nodeName;
    }

    @Deprecated
    SingleDirection(String nodeName) {
        this.nodeName = nodeName;
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
        SingleDirection direction = (SingleDirection) peer;
        return super.equals(direction) && nodeName.equals(direction.nodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeName);
    }

}
