package org.moera.node.api.node;

public class NodeApiException extends Exception {

    public NodeApiException(String message) {
        super("Node API call exception: " + message);
    }

    public NodeApiException(String message, Throwable cause) {
        super("Node API call exception: " + message + ": " + cause.getMessage(), cause);
    }

    public NodeApiException(Throwable cause) {
        super("Node API call exception: " + cause.getMessage(), cause);
    }

}
