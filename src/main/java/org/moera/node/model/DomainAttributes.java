package org.moera.node.model;

import java.util.UUID;

import org.moera.node.model.constraint.Hostname;

public class DomainAttributes {

    @Hostname
    private String name;

    private UUID nodeId;

    public DomainAttributes() {
    }

    public DomainAttributes(String name, UUID nodeId) {
        this.name = name;
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

}
