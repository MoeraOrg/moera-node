package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "media_file_previews")
public class MediaFilePreview {

    @Id
    private UUID id;

    @ManyToOne
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

}
