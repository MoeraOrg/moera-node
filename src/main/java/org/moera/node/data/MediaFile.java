package org.moera.node.data;

import java.awt.Dimension;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.media.MimeUtil;
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

    @Size(max = 50)
    private String fileName;

    @Size(max = 65)
    private String cloudFileName;

    private Timestamp cloudUploadDeadline;

    @Column(name="size_x")
    private Integer sizeX;

    @Column(name="size_y")
    private Integer sizeY;

    @NotNull
    private short orientation = 1;

    private long fileSize;

    private Float duration;

    private String streamInfo;

    @NotNull
    private boolean uncompressed;

    @ManyToOne(fetch = FetchType.LAZY)
    private MediaFile compressedFile;

    @ManyToOne(fetch = FetchType.LAZY)
    private PendingJob compressionJob;

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

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "mediaFile")
    private Set<MediaFileOwner> owners = new HashSet<>();

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "originalMediaFile")
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

    public boolean isImage() {
        return MimeUtil.isSupportedImage(mimeType);
    }

    public boolean isReasonableImage() {
        return MimeUtil.isReasonableImage(mimeType, sizeX, sizeY, fileSize);
    }

    public boolean isVideo() {
        return MimeUtil.isSupportedVideo(mimeType);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCloudFileName() {
        return cloudFileName;
    }

    public void setCloudFileName(String cloudFileName) {
        this.cloudFileName = cloudFileName;
    }

    public Timestamp getCloudUploadDeadline() {
        return cloudUploadDeadline;
    }

    public void setCloudUploadDeadline(Timestamp cloudUploadDeadline) {
        this.cloudUploadDeadline = cloudUploadDeadline;
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

    public Float getDuration() {
        return duration;
    }

    public void setDuration(Float duration) {
        this.duration = duration;
    }

    public String getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(String streamInfo) {
        this.streamInfo = streamInfo;
    }

    public boolean isUncompressed() {
        return uncompressed;
    }

    public void setUncompressed(boolean uncompressed) {
        this.uncompressed = uncompressed;
    }

    public MediaFile getCompressedFile() {
        return compressedFile;
    }

    public void setCompressedFile(MediaFile compressedFile) {
        this.compressedFile = compressedFile;
    }

    public PendingJob getCompressionJob() {
        return compressionJob;
    }

    public void setCompressionJob(PendingJob compressionJob) {
        this.compressionJob = compressionJob;
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
        MediaFilePreview smallest = null;
        MediaFilePreview largest = null;
        for (MediaFilePreview preview : getPreviews()) {
            if (preview.getMediaFile() == null) {
                continue;
            }
            if (preview.getWidth() >= width && (smallest == null || smallest.getWidth() > preview.getWidth())) {
                smallest = preview;
            }
            if (largest == null || largest.getWidth() < preview.getWidth()) {
                largest = preview;
            }
        }
        return smallest != null ? smallest : (isImage() ? null : largest);
    }

}
