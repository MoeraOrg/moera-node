package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "domains")
public class Domain {

    @Id
    @Size(max = 63)
    private String name;

    @NotNull
    private UUID nodeId;

    public Domain() {
    }

    public Domain(String name, UUID nodeId) {
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
