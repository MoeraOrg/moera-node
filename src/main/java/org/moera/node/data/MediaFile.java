package org.moera.node.data;

import java.awt.Dimension;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @NotNull
    private short orientation = 1;

    private long fileSize;

    @NotNull
    private boolean exposed;

    private byte[] digest;

    @NotNull
    private Timestamp createdAt = Util.now();

    private String recognizedText;

    private Timestamp recognizeAt;

    private Timestamp recognizedAt;

    @NotNull
    @Column(insertable = false, updatable = false)
    private int usageCount;

    @Column(insertable = false, updatable = false)
    private Timestamp deadline;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "mediaFile")
    private Set<MediaFileOwner> owners = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "originalMediaFile")
    private Set<MediaFilePreview> previews = new HashSet<>();

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

    public Dimension getDimension() {
        return new Dimension(getSizeX(), getSizeY());
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

    public short getOrientation() {
        return orientation;
    }

    public void setOrientation(short orientation) {
        this.orientation = orientation;
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

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public Timestamp getRecognizeAt() {
        return recognizeAt;
    }

    public void setRecognizeAt(Timestamp recognizeAt) {
        this.recognizeAt = recognizeAt;
    }

    public Timestamp getRecognizedAt() {
        return recognizedAt;
    }

    public void setRecognizedAt(Timestamp recognizedAt) {
        this.recognizedAt = recognizedAt;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Timestamp getDeadline() {
        return deadline;
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

    public Set<MediaFilePreview> getPreviews() {
        return previews;
    }

    public void setPreviews(Set<MediaFilePreview> previews) {
        this.previews = previews;
    }

    public void addPreview(MediaFilePreview preview) {
        previews.add(preview);
        preview.setOriginalMediaFile(this);
    }

    public void removePreview(MediaFilePreview preview) {
        previews.removeIf(sr -> sr.getId().equals(preview.getId()));
        preview.setOriginalMediaFile(null);
    }

    public MediaFilePreview findLargerPreview(int width) {
        MediaFilePreview larger = null;
        for (MediaFilePreview preview : getPreviews()) {
            if (preview.getMediaFile() == null) {
                continue;
            }
            if (preview.getWidth() >= width && (larger == null || larger.getWidth() > preview.getWidth())) {
                larger = preview;
            }
        }
        return larger;
    }

}
