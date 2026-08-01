package org.moera.node.media.video;

import java.io.IOException;

public class InvalidVideoException extends IOException {

    public InvalidVideoException() {
        super("Invalid video file");
    }

}
