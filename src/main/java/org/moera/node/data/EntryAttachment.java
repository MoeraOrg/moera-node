package org.moera.node.data;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.moera.node.media.LocalRemoteMedia;

@Entity
@Table(name = "entry_attachments")
public class EntryAttachment {

    @Id
    private UUID id;

    @ManyToOne
    private EntryRevision entryRevision;

    @ManyToOne
    private Draft draft;

    @ManyToOne
    private MediaFileOwner mediaFileOwner;

    @ManyToOne
    private MediaLease mediaFileLease;

    @ManyToOne
    private RemoteMediaFile remoteMediaFile;

    @NotNull
    private int ordinal;

    @NotNull
    private boolean embedded = true;

    public EntryAttachment() {
    }

    public EntryAttachment(
        EntryRevision entryRevision, MediaFileOwner mediaFileOwner, RemoteMediaFile remoteMediaFile, int ordinal
    ) {
        this.id = UUID.randomUUID();
        this.entryRevision = entryRevision;
        this.mediaFileOwner = mediaFileOwner;
        this.remoteMediaFile = remoteMediaFile;
        this.ordinal = ordinal;
    }

    public EntryAttachment(EntryRevision entryRevision, MediaFileOwner mediaFileOwner, int ordinal) {
        this(entryRevision, mediaFileOwner, null, ordinal);
    }

    public EntryAttachment(Draft draft, MediaFileOwner mediaFileOwner, int ordinal) {
        this.id = UUID.randomUUID();
        this.draft = draft;
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

    public Draft getDraft() {
        return draft;
    }

    public void setDraft(Draft draft) {
        this.draft = draft;
    }

    public MediaFileOwner getMediaFileOwner() {
        return mediaFileOwner;
    }

    public void setMediaFileOwner(MediaFileOwner mediaFileOwner) {
        this.mediaFileOwner = mediaFileOwner;
    }

    public MediaLease getMediaFileLease() {
        return mediaFileLease;
    }

    public void setMediaFileLease(MediaLease mediaFileLease) {
        this.mediaFileLease = mediaFileLease;
    }

    public RemoteMediaFile getRemoteMediaFile() {
        return remoteMediaFile;
    }

    public void setRemoteMediaFile(RemoteMediaFile remoteMediaFile) {
        this.remoteMediaFile = remoteMediaFile;
    }

    public LocalRemoteMedia getLocalRemoteMedia() {
        return new LocalRemoteMedia(getMediaFileOwner(), getRemoteMediaFile());
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

}
