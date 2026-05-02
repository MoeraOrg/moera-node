package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.media.MimeUtil;
import org.moera.node.util.Util;

@Entity
@Table(name = "media_file_owners")
public class MediaFileOwner {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 63)
    private String ownerName;

    @Size(max = 255)
    private String title;

    @ManyToOne
    @NotNull
    private MediaFile mediaFile;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentMedia")
    private Set<Posting> postings = new HashSet<>();

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    @Column(insertable = false, updatable = false)
    private int usageCount;

    @NotNull
    @Column(insertable = false, updatable = false)
    private Timestamp usageUpdatedAt = Util.now();

    @Column(insertable = false, updatable = false)
    private Timestamp deadline;

    @NotNull
    private boolean unrestricted;

    @NotNull
    private Timestamp permissionsUpdatedAt = Util.now();

    @NotNull
    private String malwareMarks = "";

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return MimeUtil.fileName(id.toString(), getMediaFile().getMimeType());
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public Set<Posting> getPostings() {
        return postings;
    }

    public void setPostings(Set<Posting> postings) {
        this.postings = postings;
    }

    public Posting getPostingByParentMediaEntry(Entry parentMediaEntry) {
        UUID parentMediaEntryId = parentMediaEntry != null ? parentMediaEntry.getId() : null;
        return postings.stream()
                .filter(p -> Objects.equals(
                    p.getParentMediaEntry() != null ? p.getParentMediaEntry().getId() : null,
                    parentMediaEntryId
                ))
                .findFirst()
                .orElse(null);
    }

    public void addPosting(Posting posting) {
        addPosting(posting, null);
    }

    public void addPosting(Posting posting, Entry parentMediaEntry) {
        postings.add(posting);
        posting.setParentMedia(this);
        posting.setParentMediaEntry(parentMediaEntry);
    }

    public void removePosting(Posting posting) {
        postings.removeIf(sr -> sr.getId().equals(posting.getId()));
        posting.setParentMedia(null);
        posting.setParentMediaEntry(null);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Timestamp getUsageUpdatedAt() {
        return usageUpdatedAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public boolean isUnrestricted() {
        return unrestricted;
    }

    public void setUnrestricted(boolean unrestricted) {
        this.unrestricted = unrestricted;
    }

    public Timestamp getPermissionsUpdatedAt() {
        return permissionsUpdatedAt;
    }

    public void setPermissionsUpdatedAt(Timestamp permissionsUpdatedAt) {
        this.permissionsUpdatedAt = permissionsUpdatedAt;
    }

    public String getMalwareMarks() {
        return malwareMarks;
    }

    public void setMalwareMarks(String malwareMarks) {
        this.malwareMarks = malwareMarks;
    }

}
