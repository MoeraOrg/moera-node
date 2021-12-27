package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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

    @OneToOne(mappedBy = "parentMedia")
    private Posting posting;

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

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        if (this.posting != null) {
            this.posting.setParentMedia(null);
        }
        this.posting = posting;
        if (posting != null) {
            posting.setParentMedia(this);
        }
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
