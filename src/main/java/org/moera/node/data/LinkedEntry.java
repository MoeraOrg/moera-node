package org.moera.node.data;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "linked_entries")
public class LinkedEntry {

    @Id
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private EntryRevision entryRevision;

    @NotNull
    @Size(max = 135)
    private String receiverNodeName;

    @NotNull
    @Size(max = 40)
    private String receiverEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Entry linkedEntry;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EntryRevision getEntryRevision() {
        return entryRevision;
    }

    public void setEntryRevision(EntryRevision entryRevision) {
        this.entryRevision = entryRevision;
    }

    public String getReceiverNodeName() {
        return receiverNodeName;
    }

    public void setReceiverNodeName(String receiverNodeName) {
        this.receiverNodeName = receiverNodeName;
    }

    public String getReceiverEntryId() {
        return receiverEntryId;
    }

    public void setReceiverEntryId(String receiverEntryId) {
        this.receiverEntryId = receiverEntryId;
    }

    public Entry getLinkedEntry() {
        return linkedEntry;
    }

    public void setLinkedEntry(Entry linkedEntry) {
        this.linkedEntry = linkedEntry;
    }

}
