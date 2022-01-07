package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.media.MimeUtils;
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

    @Column(insertable = false, updatable = false)
    private Timestamp deadline;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return MimeUtils.fileName(id.toString(), getMediaFile().getMimeType());
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

    public Posting getPosting(String receiverName) {
        return postings.stream()
                .filter(p -> Objects.equals(p.getReceiverName(), receiverName))
                .findFirst()
                .orElse(null);
    }

    public void addPosting(Posting posting) {
        postings.add(posting);
        posting.setParentMedia(this);
    }

    public void removePosting(Posting posting) {
        postings.removeIf(sr -> sr.getId().equals(posting.getId()));
        posting.setParentMedia(null);
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

    public Timestamp getDeadline() {
        return deadline;
    }

}
