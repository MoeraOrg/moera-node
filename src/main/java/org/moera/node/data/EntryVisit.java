package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "entry_visits")
public class EntryVisit {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Entry entry;

    @Size(max = 24)
    private String clientId;

    @Size(max = 135)
    private String clientName;

    @NotNull
    private Timestamp visitedAt = Util.now();

    @NotNull
    private Timestamp deadline;

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

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Timestamp getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Timestamp visitedAt) {
        this.visitedAt = visitedAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

}
