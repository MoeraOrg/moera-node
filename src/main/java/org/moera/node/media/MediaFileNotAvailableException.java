package org.moera.node.media;

import java.io.IOException;

public class MediaFileNotAvailableException extends IOException {

    public MediaFileNotAvailableException(String mediaFileId) {
        super("Local copy of media file %s is not available".formatted(mediaFileId));
    }

}
