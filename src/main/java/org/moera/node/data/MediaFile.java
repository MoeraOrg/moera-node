package org.moera.node.data;

import java.awt.Dimension;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.media.MimeUtils;
import org.moera.node.util.Util;

@Entity
@Table(name = "media_files")
public class MediaFile {

    @Id
    @Size(max = 40)
    private String id;

    @NotNull
    @Size(max = 80)
    private String mimeType;

    @Column(name="size_x")
    private Integer sizeX;

    @Column(name="size_y")
    private Integer sizeY;

    private long fileSize;

    @NotNull
    private boolean exposed;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    @Column(insertable = false, updatable = false)
    private int usageCount;

    @Column(insertable = false, updatable = false)
    private Timestamp deadline;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "mediaFile")
    private Set<MediaFileOwner> owners = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return MimeUtils.fileName(id, mimeType);
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

    public void setDimension(Dimension dimension) {
        if (dimension != null) {
            setSizeX(dimension.width);
            setSizeY(dimension.height);
        } else {
            setSizeX(null);
            setSizeY(null);
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
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

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Set<MediaFileOwner> getOwners() {
        return owners;
    }

    public void setOwners(Set<MediaFileOwner> owners) {
        this.owners = owners;
    }

    public void addOwner(MediaFileOwner owner) {
        owners.add(owner);
        owner.setMediaFile(this);
    }

    public void removeOwner(MediaFileOwner owner) {
        owners.removeIf(sr -> sr.getId().equals(owner.getId()));
        owner.setMediaFile(null);
    }

}
