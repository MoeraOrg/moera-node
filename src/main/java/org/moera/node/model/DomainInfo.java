package org.moera.node.model;

import java.util.UUID;

import org.moera.node.model.constraint.Hostname;
import org.moera.node.model.constraint.Uuid;

public class DomainInfo {

    @Hostname
    private String name;

    @Uuid
    private String nodeId;

    public DomainInfo() {
    }

    public DomainInfo(String name, String nodeId) {
        this.name = name;
        this.nodeId = nodeId;
    }

    public DomainInfo(String name, UUID nodeId) {
        this(name, nodeId.toString());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

}
