package org.moera.node.plugin;

import java.util.UUID;

public class PluginDescriptor {

    private UUID nodeId;
    private String name;

    public PluginDescriptor(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
