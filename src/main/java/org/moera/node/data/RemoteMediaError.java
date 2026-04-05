package org.moera.node.data;

public enum RemoteMediaError {

    DIGEST_INCORRECT("media.digest-incorrect"),
    DOWNLOAD_FAILED("media.download-failed"),
    STORAGE_ERROR("media.storage-error");

    private final String errorCode;

    RemoteMediaError(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
