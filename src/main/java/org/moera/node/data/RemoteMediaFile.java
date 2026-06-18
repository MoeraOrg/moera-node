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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "remote_media_files")
public class RemoteMediaFile {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 135)
    private String nodeName;

    @NotNull
    @Size(max = 40)
    private String mediaId;

    @Size(max = 40)
    private String hash;

    private byte[] digest;

    @Size(max = 80)
    private String mimeType;

    @NotNull
    private boolean attachment;

    @Column(name="size_x")
    private Integer sizeX;

    @Column(name="size_y")
    private Integer sizeY;

    private Long fileSize;

    @Size(max = 255)
    private String title;

    @Size(max = 40)
    private String leaseId;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "parentRemoteMedia")
    private Set<Posting> postings = new HashSet<>();

    @NotNull
    @Column(insertable = false, updatable = false)
    private int usageCount;

    @Column(insertable = false, updatable = false)
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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isAttachment() {
        return attachment;
    }

    public void setAttachment(boolean attachment) {
        this.attachment = attachment;
    }

    public Integer getSizeX() {
        return sizeX;
    }

    public void setSizeX(Integer sizeX) {
        this.sizeX = sizeX;
    }

    public Integer getSizeY() {
        return sizeY;
    }

    public void setSizeY(Integer sizeY) {
        this.sizeY = sizeY;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
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
        postings.add(posting);
        posting.setParentRemoteMedia(this);
    }

    public void removePosting(Posting posting) {
        postings.removeIf(sr -> sr.getId().equals(posting.getId()));
        posting.setParentRemoteMedia(null);
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

}
