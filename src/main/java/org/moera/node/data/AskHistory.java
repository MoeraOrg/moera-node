package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.AskSubject;
import org.moera.node.util.Util;

@Entity
@Table(name = "ask_history")
public class AskHistory {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @NotNull
    @Enumerated
    private AskSubject subject;

    @NotNull
    private Timestamp createdAt = Util.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public AskSubject getSubject() {
        return subject;
    }

    public void setSubject(AskSubject subject) {
        this.subject = subject;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

}
