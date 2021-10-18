package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "entry_attachments")
public class EntryAttachment {

    @Id
    private UUID id;

    @ManyToOne
    @NotNull
    private EntryRevision entryRevision;

    @ManyToOne
    @NotNull
    private MediaFileOwner mediaFileOwner;

    @NotNull
    private int ordinal;

    public EntryAttachment() {
    }

    public EntryAttachment(EntryRevision entryRevision, MediaFileOwner mediaFileOwner, int ordinal) {
        this.id = UUID.randomUUID();
        this.entryRevision = entryRevision;
        this.mediaFileOwner = mediaFileOwner;
        this.ordinal = ordinal;
    }

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

    public MediaFileOwner getMediaFileOwner() {
        return mediaFileOwner;
    }

    public void setMediaFileOwner(MediaFileOwner mediaFileOwner) {
        this.mediaFileOwner = mediaFileOwner;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

}