package org.moera.node.model;

import java.util.UUID;
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
