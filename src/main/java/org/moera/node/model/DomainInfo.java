package org.moera.node.model;

import javax.validation.constraints.NotBlank;

public class DomainInfo {

    @NotBlank
    private String name;

    private String nodeId;

    public DomainInfo() {
    }

    public DomainInfo(String name, String nodeId) {
        this.name = name;
        this.nodeId = nodeId;
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
