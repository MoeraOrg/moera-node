package org.moera.node.media;

public class TemporaryMediaFile {

    private final String mediaFileId;
    private final String contentType;

    public TemporaryMediaFile(String mediaFileId, String contentType) {
        this.mediaFileId = mediaFileId;
        this.contentType = contentType;
    }

    public String getMediaFileId() {
        return mediaFileId;
    }

    public String getContentType() {
        return contentType;
    }

}
