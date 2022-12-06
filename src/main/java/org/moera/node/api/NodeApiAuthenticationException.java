package org.moera.node.api;

public class NodeApiAuthenticationException extends NodeApiErrorStatusException {

    public NodeApiAuthenticationException() {
        super("Authentication required");
    }

}
