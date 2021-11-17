package org.moera.node.media;

public class TemporaryMediaFile {

    private final String mediaFileId;
    private final String contentType;
    private final byte[] digest;

    public TemporaryMediaFile(String mediaFileId, String contentType, byte[] digest) {
        this.mediaFileId = mediaFileId;
        this.contentType = contentType;
        this.digest = digest;
    }

    public String getMediaFileId() {
        return mediaFileId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getDigest() {
        return digest;
    }

}
