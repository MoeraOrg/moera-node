package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.model.RemoteMedia;
import org.moera.node.util.Util;

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

    @Size(max = 40)
    private String remoteMediaId;

    @Size(max = 40)
    private String remoteMediaHash;

    private byte[] remoteMediaDigest;

    @NotNull
    private int ordinal;

    @NotNull
    private boolean embedded = true;

    public EntryAttachment() {
    }

    public EntryAttachment(EntryRevision entryRevision, MediaFileOwner mediaFileOwner, int ordinal) {
        this.id = UUID.randomUUID();
        this.entryRevision = entryRevision;
        this.mediaFileOwner = mediaFileOwner;
        this.ordinal = ordinal;
    }

    public EntryAttachment(Draft draft, MediaFileOwner mediaFileOwner, int ordinal) {
        this.id = UUID.randomUUID();
        this.draft = draft;
        this.mediaFileOwner = mediaFileOwner;
        this.ordinal = ordinal;
    }

    public EntryAttachment(Draft draft, RemoteMedia remoteMedia, int ordinal) {
        this.id = UUID.randomUUID();
        this.draft = draft;
        this.remoteMediaId = remoteMedia.getId();
        this.remoteMediaHash = remoteMedia.getHash();
        this.remoteMediaDigest = Util.base64decode(remoteMedia.getDigest());
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

    public String getRemoteMediaId() {
        return remoteMediaId;
    }

    public void setRemoteMediaId(String remoteMediaId) {
        this.remoteMediaId = remoteMediaId;
    }

    public String getRemoteMediaHash() {
        return remoteMediaHash;
    }

    public void setRemoteMediaHash(String remoteMediaHash) {
        this.remoteMediaHash = remoteMediaHash;
    }

    public byte[] getRemoteMediaDigest() {
        return remoteMediaDigest;
    }

    public void setRemoteMediaDigest(byte[] remoteMediaDigest) {
        this.remoteMediaDigest = remoteMediaDigest;
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
