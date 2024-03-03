package org.moera.node.api.node;

public class NodeApiAuthenticationException extends NodeApiErrorStatusException {

    public NodeApiAuthenticationException() {
        super("Authentication required");
    }

}
