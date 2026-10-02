package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.SharingType;
import org.moera.node.util.Util;

@Entity
@Table(name = "entry_sharings")
public class EntrySharing {

    @Id
    private UUID id;

    @NotNull
    @Enumerated
    private SharingType sharingType;

    @NotNull
    @Size(max = 135)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteFullName;

    @Size(max = 1024)
    private String remoteSourceUri;

    @Size(max = 31)
    private String remoteGender;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 40)
    private String remoteStoryId;

    private Long remoteStoryMoment;

    @Size(max = 40)
    private String remotePostingId;

    @NotNull
    @ManyToOne
    private Entry entry;

    @ManyToOne
    private EntryRevision entryRevision;

    @NotNull
    private Timestamp createdAt = Util.now();

    private long moment;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SharingType getSharingType() {
        return sharingType;
    }

    public void setSharingType(SharingType sharingType) {
        this.sharingType = sharingType;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public String getRemoteSourceUri() {
        return remoteSourceUri;
    }

    public void setRemoteSourceUri(String remoteSourceUri) {
        this.remoteSourceUri = remoteSourceUri;
    }

    public String getRemoteGender() {
        return remoteGender;
    }

    public void setRemoteGender(String remoteGender) {
        this.remoteGender = remoteGender;
    }

    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
    }

    public String getRemoteAvatarShape() {
        return remoteAvatarShape;
    }

    public void setRemoteAvatarShape(String remoteAvatarShape) {
        this.remoteAvatarShape = remoteAvatarShape;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemoteStoryId() {
        return remoteStoryId;
    }

    public void setRemoteStoryId(String remoteStoryId) {
        this.remoteStoryId = remoteStoryId;
    }

    public Long getRemoteStoryMoment() {
        return remoteStoryMoment;
    }

    public void setRemoteStoryMoment(Long remoteStoryMoment) {
        this.remoteStoryMoment = remoteStoryMoment;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

}
