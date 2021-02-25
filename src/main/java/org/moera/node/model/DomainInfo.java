package org.moera.node.model;

import org.moera.node.data.Domain;
import org.moera.node.util.Util;

public class DomainInfo {

    private String name;
    private String nodeId;
    private long createdAt;

    public DomainInfo() {
    }

    public DomainInfo(Domain domain) {
        this.name = domain.getName();
        this.nodeId = domain.getNodeId().toString();
        this.createdAt = Util.toEpochSecond(domain.getCreatedAt());
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
