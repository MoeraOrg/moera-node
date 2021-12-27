package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.MediaFileOwner;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateMediaFileInfo {

    private String id;
    private String hash;
    private String path;
    private Integer width;
    private Integer height;
    private long size;
    private String postingId;
    private MediaFilePreviewInfo[] previews;

    public PrivateMediaFileInfo() {
    }

    public PrivateMediaFileInfo(MediaFileOwner mediaFileOwner) {
        id = mediaFileOwner.getId().toString();
        hash = mediaFileOwner.getMediaFile().getId();
        path = "private/" + mediaFileOwner.getFileName();
        width = mediaFileOwner.getMediaFile().getSizeX();
        height = mediaFileOwner.getMediaFile().getSizeY();
        size = mediaFileOwner.getMediaFile().getFileSize();
        postingId = mediaFileOwner.getPosting() != null ? mediaFileOwner.getPosting().getId().toString() : null;
        previews = mediaFileOwner.getMediaFile().getPreviews().stream()
                .filter(pw -> pw.getMediaFile() != null)
                .map(MediaFilePreviewInfo::new)
                .toArray(MediaFilePreviewInfo[]::new);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public MediaFilePreviewInfo[] getPreviews() {
        return previews;
    }

    public void setPreviews(MediaFilePreviewInfo[] previews) {
        this.previews = previews;
    }

    public MediaFilePreviewInfo findLargerPreview(int width) {
        MediaFilePreviewInfo larger = null;
        for (MediaFilePreviewInfo preview : getPreviews()) {
            if (preview.getWidth() >= width && (larger == null || larger.getWidth() > preview.getWidth())) {
                larger = preview;
            }
        }
        return larger;
    }

}
