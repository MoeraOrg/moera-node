package org.moera.node.linkpreviewnet;

public class LinkPreviewNetException extends Exception {

    public LinkPreviewNetException(String message) {
        super("LinkPreview.net API exception: " + message);
    }

    public LinkPreviewNetException(String message, Throwable cause) {
        super("LinkPreview.net API exception: " + message + ": " + cause.getMessage(), cause);
    }

    public LinkPreviewNetException(Throwable cause) {
        super("LinkPreview.net API exception: " + cause.getMessage(), cause);
    }

}
