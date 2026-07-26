package org.moera.node.media;

import java.io.IOException;

public class MediaFileNotAvailableException extends IOException {

    public MediaFileNotAvailableException(String mediaFileId) {
        super(getMessage(mediaFileId));
    }

    public MediaFileNotAvailableException(String mediaFileId, Throwable cause) {
        super(getMessage(mediaFileId), cause);
    }

    private static String getMessage(String mediaFileId) {
        return "Media file %s is not available".formatted(mediaFileId);
    }

}
