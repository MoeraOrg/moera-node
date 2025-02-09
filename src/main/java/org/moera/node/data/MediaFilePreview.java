package org.moera.node.data;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

import org.moera.node.util.MediaUtil;

@Entity
@Table(name = "media_file_previews")
public class MediaFilePreview {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private MediaFile originalMediaFile;

    @NotNull
    private int width;

    @ManyToOne
    private MediaFile mediaFile;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MediaFile getOriginalMediaFile() {
        return originalMediaFile;
    }

    public void setOriginalMediaFile(MediaFile originalMediaFile) {
        this.originalMediaFile = originalMediaFile;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    @Transient
    public boolean isOriginal() {
        return mediaFile != null && mediaFile.getId().equals(originalMediaFile.getId());
    }

    public String getDirectFileName(String originalDirectFileName) {
        if (originalDirectFileName != null) {
            return isOriginal() ? originalDirectFileName : MediaUtil.mediaPreviewDirect(originalDirectFileName, width);
        } else {
            return null;
        }
    }

    public String getDirectPath(String originalDirectPath) {
        if (originalDirectPath != null) {
            return isOriginal() ? originalDirectPath : MediaUtil.mediaPreviewDirect(originalDirectPath, width);
        } else {
            return null;
        }
    }

}
