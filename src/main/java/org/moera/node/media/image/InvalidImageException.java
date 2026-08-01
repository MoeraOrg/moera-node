package org.moera.node.media.image;

import java.io.IOException;

public class InvalidImageException extends IOException {

    public InvalidImageException() {
        super("Invalid media file");
    }

}
