package org.moera.node.media;

public class MediaPathNotSetException extends Exception {

    public MediaPathNotSetException(String message) {
        super("Media storage error: " + message + ". Set node.media.path in the configuration to the path to"
                + " a writable directory for storing media files");
    }

}
