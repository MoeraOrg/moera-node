package org.moera.node.api;

public class NodeApiUnknownNameException extends NodeApiException {

    public NodeApiUnknownNameException(String nodeName) {
        super(String.format("Node name '%s' not found", nodeName));
    }

}
