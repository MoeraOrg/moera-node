package org.moera.node.liberin;

import java.util.UUID;

public class Liberin {

    private UUID nodeId;
    private String clientId;

    public Liberin() {
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
