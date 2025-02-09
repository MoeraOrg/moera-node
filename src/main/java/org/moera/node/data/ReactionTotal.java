package org.moera.node.data;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "reaction_totals")
public class ReactionTotal {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    private EntryRevision entryRevision;

    @NotNull
    private boolean negative;

    @NotNull
    private int emoji;

    @NotNull
    private int total;

    @NotNull
    private boolean forged;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public EntryRevision getEntryRevision() {
        return entryRevision;
    }

    public void setEntryRevision(EntryRevision entryRevision) {
        this.entryRevision = entryRevision;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isForged() {
        return forged;
    }

    public void setForged(boolean forged) {
        this.forged = forged;
    }

}
