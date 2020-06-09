package org.moera.node.api;

public class NodeApiErrorStatusException extends NodeApiException {

    protected NodeApiErrorStatusException(String message) {
        super(message);
    }

    public NodeApiErrorStatusException(int status, String body) {
        super(String.format("Error status returned: %d (body: %s)", status, body));
    }

}
