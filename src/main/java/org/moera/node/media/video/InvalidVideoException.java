package org.moera.node.media.video;

import java.io.IOException;

public class InvalidVideoException extends IOException {

    public InvalidVideoException(String message) {
        super("Invalid video file: " + message);
    }

    public InvalidVideoException(String message, Throwable cause) {
        super("Invalid video file: " + message, cause);
    }

}
